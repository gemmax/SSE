package indexer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class BasicIndex implements BaseIndex {
	private static final int INT_SIZE = Integer.SIZE / Byte.SIZE;

	public PostingList readPosting(FileChannel fc) throws Throwable {
		/*
		 * Allocate two ints, preparing for reading in termId and freq
		 */
		ByteBuffer buffer = ByteBuffer.allocate(INT_SIZE * 2);
		int numOfBytesRead;
		
		/*
		 * fc.read reads a sequence of bytes from the fc channel into 
		 * buffer. Bytes are read starting at this channel's current 
		 * file position, and then the file position is updated 
		 * with the number of bytes actually read. 
		 */

			numOfBytesRead = fc.read(buffer);
			if (numOfBytesRead == -1) return null;

		/*
		 * Rewinds the buffer. Position is set to zero. 
		 * We are ready to get our termId and frequency.
		 */
		buffer.rewind();
		/*
		 * Reads the next four bytes at buffer's current position, 
		 * composing them into an int value according to the 
		 * current byte order, and then increments the position 
		 * by four.
		 */	
		int termId = buffer.getInt();
		int freq = buffer.getInt();
		
		/* TODO: yeah
		 * You should create a PostingList and use buffer 
		 * to fill it with docIds, then return the PostingList 
		 * you created.
		 * Hint: This differs from reading in termId/freq only 
		 * in the number of ints to be read in.
		 */
		buffer = ByteBuffer.allocate(INT_SIZE * freq);
		if (fc.read(buffer) == -1) return null;
		buffer.rewind();

		PostingList postingList = new PostingList(termId);
		for (int i = 0; i < freq; i++) {
			postingList.getList().add(buffer.getInt());
		}
		return postingList;
	}

	public void writePosting(FileChannel fc, PostingList p) throws Throwable {
		/*
		 * The allocated space is for termID + freq + docIds in p
		 */
		ByteBuffer buffer = ByteBuffer.allocate(INT_SIZE * (p.getList().size() + 2));
		buffer.putInt(p.getTermId()); // put termId
		buffer.putInt(p.getList().size()); // put freq
		for (int id : p.getList()) { // put docIds
			buffer.putInt(id); 
		}
		
		/* Flip the buffer so that the position is set to zero.
		 * This is the counterpart of buffer.rewind()
		 */
		buffer.flip();
		/*
		 * fc.write writes a sequence of bytes into fc from buffer.
		 * File position is updated with the number of bytes actually 
		 * written
		 */
		try {
			fc.write(buffer);
		} catch (IOException e) {
			throw e;
		}
	}
}
