package indexer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;

// import org.junit.Assert;
// import org.junit.Test;

public class GammaIndex implements BaseIndex {
	private final static int VBNUM_SIZE = Integer.SIZE / (Byte.SIZE - 1) + 1;

	// caution docId != 0, because gamma algorithm cannot encode 0
	private void gammaEncode(int[] values, ByteBuffer buffer, int nums) {
		// System.out.println("...gammaEncode start");
		byte tmp = 0;
		int bitpos = 8; // used    76543210
		for (int i = 0; buffer.hasRemaining() && i < nums; i++) {
			int length;
			// System.out.println("value : " + values[i]);
			for (length = Integer.SIZE; ((values[i] >> length - 1) & 0x01) == 0; length--);
			length--; // cut the highest bit 1.
			// System.out.println("useful length : " + length);

			for (int j = 1; j <= length; j++) {
				tmp = (byte)((tmp << 1) | 0x01);
				bitpos --;
				if (bitpos == 0) {
					buffer.put(tmp);
					tmp = 0;
					bitpos = 8;
				}
			}
			tmp = (byte)(tmp << 1);
			bitpos--;
			if (bitpos ==0) {
				buffer.put(tmp);
				tmp = 0;
				bitpos = 8;
			}
			if (length == 0) continue;

			// ignore the highest bit of 1. use length to get pointed bit of integer.
			// System.out.println("bitpos : " + bitpos);
			// System.out.println("useful length : " + length);
			for (int j = length; j > 0; j--) {
				tmp = (byte) ((tmp << 1) | ((values[i] >> j - 1) & 0x01));
				bitpos--;
				if (bitpos == 0) {
					buffer.put(tmp);
					tmp = 0;
					bitpos = 8;
				}
			}
		}

		if (bitpos != 8) {
			tmp = (byte)(tmp << bitpos);
			buffer.put(tmp);
		}
		// System.out.println("gammaEncode end...");
	}

	// the last item is bit position
	private int[] gammaDecode(ByteBuffer buffer, int nums, int usedBitpos) {
		int [] results = new int[nums + 1];
		int bitpos = usedBitpos; // for byte : 76543210  next bit means unvisited
		byte tmp;
		for (int i = 0; buffer.hasRemaining() && i < nums; i++) {
			if (bitpos != 8) { // a new byte
				buffer.position(buffer.position() - 1);
			}
			// System.out.println("buffer position : " + buffer.position() + " bit pos: " + bitpos);
			tmp = buffer.get();
			bitpos--;

			int length = 0; // length of bits
			for (; ((tmp >> bitpos) & 0x01) == 1; bitpos--){
				length++;
				// if all bits of byte are 1
				if (bitpos == 0) {
					// load a new byte
					tmp = buffer.get();
					bitpos = 8;
				}
			}
			// System.out.println("length : " + length);

			// Caution
			if (length == 0) {
				results[i] = 1;
				if (bitpos == 0) {
					bitpos = 8;
				}
				continue;
			}
			// if the condition is : 11111110
			if (bitpos == 0) {
				tmp = buffer.get();
				bitpos = 8;
			}
			// now bitpos point to first zero
			// minus one
			bitpos--;

			// now bitpos point to real num
			// set the ignore highest 1.
			int num = 1;

			for (int j = 0; j < length - 1; j++, bitpos--) {
				num = (num << 1) + ((tmp >> bitpos) & 0x01);
				if (bitpos == 0) {
					tmp = buffer.get(); // load
					bitpos = 8;
				}
			}
			num = (num << 1) + (tmp >> bitpos & 0x01); // now bitpos point the last used bit
			if (bitpos == 0) bitpos = 8;  //Caution
			results[i] = num;
		}
		results[nums] = bitpos;
		return results;
	}

	private void gapEncode(PostingList postingList, ByteBuffer buffer) {
		// System.out.println("...gapEncode start");
		if (postingList == null) return;
		int [] saves = new int[postingList.getList().size() + 2];
		saves[0] = postingList.getTermId();
		saves[1] = postingList.getList().size();
		if (saves[1] > 0) {
			Iterator<Integer> p = postingList.getList().iterator();
			int pre = popNextOrNull(p);
			saves[2] = pre;
			for (int i = 3; p.hasNext() && i < (saves[1] + 2); i++) {
				int tmp = popNextOrNull(p);
				saves[i] = tmp - pre;
				pre = tmp;
				// System.out.println("gap : "+saves[i]);
			}
		}
		gammaEncode(saves, buffer, saves[1] + 2);
		// System.out.println("gapEncode end...");
	}

	/**
	 * @param size the size of docId list.
	 * @param bitpos the used bit pointer.
	 */
	private PostingList gapDecode(int size, ByteBuffer buffer, int bitpos, int termId) {
		if (size == 0) return new PostingList(termId); //todo add 21
		ArrayList<Integer> list = new ArrayList<Integer>();
		int [] gaps = gammaDecode(buffer, size, bitpos);
		list.add(gaps[0]);
		int tmp = gaps[0];
		for (int i = 1; i < size; i++) {
			tmp = tmp + gaps[i];
			list.add(tmp);
		}
		PostingList p = new PostingList(termId, list);
		return p;
	}

	//Test Gamma Encoding
	/*
	@Test
	public void test(){
		boolean passed = true;
		int[] list = {1,23,145,1,56,45612,45,555,546545,1,1,1,154,1564,1,1,4564564,1111,14,5,4,5,6};
		// int[] list = {1564,1,1,4564564,1111,14,5,4,5,6};
		ByteBuffer bf = ByteBuffer.allocate(list.length * VBNUM_SIZE);
		gammaEncode(list, bf, list.length);
		System.out.println(bf.position());
		int lens = bf.position();
		System.out.println("Encoded");
		bf.rewind();
		for (int i = 0; i < lens; i++){
			byte b = bf.get();
			String s1 = String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
			System.out.println(s1 + " ");
		}

		bf.rewind();
		int[] results = gammaDecode(bf,list.length,8);
		for (int i = 0; i < list.length; i++) {
			if (list[i] != results[i]){
				System.out.println("Error encoding: " + list[i]);
				passed = false;
			}
		}
		if (passed){
			System.out.println("Passed!");
		}
		Assert.assertEquals(true, passed);
	}
	*/
	public PostingList readPosting(FileChannel fc) throws IOException {
		/*
		 * TODO: Your code here
		 * precondition: each postinglist use integral byte.
		 * position marks the byte should be recognized
		 */
		long position = fc.position();

		ByteBuffer buff = ByteBuffer.allocate(VBNUM_SIZE * 2);

		int numOfBytesRead;
		numOfBytesRead = fc.read(buff);
		if (numOfBytesRead == -1) return null;
		buff.rewind();

		int [] termHead = gammaDecode(buff, 2, 8); //read head

		// todo locate the pre-visit bit of byte.
		fc.position(position);

		ByteBuffer buffer = ByteBuffer.allocate(VBNUM_SIZE * (termHead[1] + 2));
		if (fc.read(buffer) == -1) return null;
		buffer.rewind();
		buffer.position(buff.position());

		PostingList result = gapDecode(termHead[1], buffer, termHead[2], termHead[0]);

		position = position + buffer.position();
		fc.position(position);
		return result;
	}

	public void writePosting(FileChannel fc, PostingList p) throws Exception {
		/*
		 * TODO: Your code here
		 */
		// System.out.println("...writePosting start");

		int size = p.getList().size();
		ByteBuffer buffer = ByteBuffer.allocate(VBNUM_SIZE * (size + 2));
		gapEncode(p, buffer);
		int limits = buffer.position();
		/* Flip the buffer so that the position is set to zero.
		 * This is the counterpart of buffer.rewind()
		 */
		buffer.flip();

		buffer.limit(limits);
		try {
			fc.write(buffer);
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}
		// System.out.println("writePosting end...");
	}
	private <X> X popNextOrNull(Iterator<X> p) {
		return p.hasNext() ? p.next() : null;
	}
}
