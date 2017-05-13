package indexer;

import util.Pair;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class VBIndex implements BaseIndex {
	private static final int VBNUM_SIZE = Integer.SIZE / (Byte.SIZE - 1) + 1;

	private byte[] vBNumber (int number) {
		Stack<Byte> vbn = new Stack<Byte>();
		while (true) {
			vbn.push(new Byte((byte)(number & 0x7f)));
			if (number < 128)
				break;
			number /= 128;
		}

		byte[] bytes = new byte[vbn.size()];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = vbn.pop();
			if (vbn.isEmpty()) {
				bytes[i] = (byte)(bytes[i] | 0x80);
			}
		}
		return bytes;
	}

	private int number (byte[] vbnumber, int vbnsize) {
		int num = 0, i;
		if (vbnsize < 1) { // means invalid
			return -1;
		}

		for (i = 0; i < vbnsize - 1; i++) {
			num = num * 128 + vbnumber[i];
		}
		// System.out.println("byte start with 1 is larger than 128? or equals minus : " + (vbnumber[i] > 128));
		// minus
		num = num * 128 + (vbnumber[i] & 0x7f); // caution! the highest bit in Integer means minus. ERROR : num -= 128.
		return num;
	}


	/*private ByteBuffer vBEncode (List<Integer> numbers) {
		ByteBuffer bb = ByteBuffer.allocate(numbers.size() * VBNUM_SIZE);
		Iterator<Integer> p = numbers.iterator();
		Integer num = popNextOrNull(p);
		while (num != null) {
			bb.put(vBNumber(num));
			num = popNextOrNull(p);
		}
		return bb;
	}*/

	private List<Integer> vBDecode (ByteBuffer bb, int size) {
		List<Integer> numbers = new ArrayList<Integer>();
		int num = 0;
		int i = 0;
		while (bb.hasRemaining() && i < size) {
			byte b = bb.get();
			if (((b >> 7) & 0x01) == 0) {
				num = num * 128 + b;
			} else {
				num = num * 128 + (b & 0x7f);
				numbers.add(num);
				i++;
				num = 0;
			}
		}
        /*System.out.println("......VB decode inside......");
		for (int a : numbers) {
		    System.out.println(a);
        }*/
		// System.out.println("vbdecode : " + bb.position());
		return numbers;
	}

	// TODO ensure write into bytebuffer. yes 16
	private byte[] gapEncode (List<Integer> list) {
	    if (list == null || list.size() == 0) {
	        return null;
        }
		Iterator<Integer> p = list.iterator();
		Integer docId = popNextOrNull(p);
		byte[] firDocId = vBNumber(docId);
		byte[] bytes = new byte[VBNUM_SIZE * list.size()];
        int index;
        for (index = 0; index < firDocId.length; index++) {
            bytes[index] = firDocId[index];
        }

	    while (docId != null) {
	        Integer nextDocId = popNextOrNull(p);
	        if (nextDocId == null) {
	            break;
            }
	        byte[] gap = vBNumber(nextDocId - docId);
	        for (int i = 0; i < gap.length; i++) {
	            bytes[index] = gap[i];
	            index++;
            }
	        docId = nextDocId;
        }

        byte[] result = new byte[index];
        for (int i = 0; i < index; i++) {
            result[i] = bytes[i];
        }



        /*System.out.println("......gap encode inside......");

        for (byte t : result) {
            for (int i = 0; i < Byte.SIZE; i++) {
                int n = t & (1 << (7 - i));
                n = n >> (7 - i);
                System.out.print(n);
            }
            System.out.println();
        }*/

        return result;
	}

	// return byte size for position
	private Pair<PostingList, Integer> gapDecode(int size, ByteBuffer list, int termId) {
		List<Integer> docList = vBDecode(list, size);
		Iterator<Integer> p = docList.iterator();
		Integer docId = popNextOrNull(p);
        List<Integer> docIds = new ArrayList<Integer>();
		if (docId != null) {
			docIds.add(docId);
			Integer gap = null;
			while ( (gap = popNextOrNull(p)) != null) {
				docId = docId + gap;
				docIds.add(docId);
			}
		}
        PostingList result = new PostingList(termId, docIds);
		Pair<PostingList, Integer> results = new Pair(result, list.position());
        /*System.out.println("......inside......");
		for (int n : docIds) {
		    System.out.println(n);
        }*/

		//System.out.println("gap : " + list.position());
        // test bytebuffer position of gapDecode equals vbdecode? wish true. yes
		return results;
	}

	public PostingList readPosting(FileChannel fc) throws IOException {
		/*
		 * TODO: Your code here
		 */
		long position = fc.position();
		ByteBuffer buffer = ByteBuffer.allocate(VBNUM_SIZE * 2);
		int numOfBytesRead;

		numOfBytesRead = fc.read(buffer);
		if (numOfBytesRead == -1) return null;


		buffer.rewind();

		// TODO check following code ! logically 20170415 yes 16

		int [] termHead = new int[2];
		for (int ns = 0; ns < 2; ns++) {
			byte[] vbnum = new byte[VBNUM_SIZE];
			byte temp = 0;
			int i = 0;
			for (; buffer.hasRemaining() && ((temp = buffer.get()) >> 7 == 0); i++) {
				vbnum[i] = temp;
			}
			vbnum[i] = temp;
			i++; //nums of byte
			termHead[ns] = number(vbnum, i);
			//termHead[ns] = number(vbnum, buffer.position());
			//size += buffer.position();
		}
		// set position after read two vb number
        position = position + buffer.position();
		fc.position(position);

		buffer = ByteBuffer.allocate(VBNUM_SIZE * termHead[1]);
        if (fc.read(buffer) == -1) return null;
        buffer.rewind();

        Pair<PostingList, Integer> results = gapDecode(termHead[1], buffer, termHead[0]);
        PostingList postingList = results.getFirst();

        position = position + results.getSecond();
		fc.position(position);
		return postingList;
	}

	public void writePosting(FileChannel fc, PostingList p) throws IOException{
		/*
		 * TODO: Your code here
		 */
		int termId = p.getTermId();
		List<Integer> docIds = p.getList();
		int size = docIds.size();
        int locme;
        byte [] vbTerm = vBNumber(termId);
		byte [] vbSize = vBNumber(size);
		byte [] vbList = gapEncode(docIds);
		if (vbList != null) {
            locme = vbTerm.length + vbSize.length + vbList.length;
        } else {
		    locme = vbTerm.length + vbSize.length;
        }

		ByteBuffer buffer = ByteBuffer.allocate(locme * Byte.SIZE);
		buffer.put(vbTerm);
		buffer.put(vbSize);
		if (vbList != null) {
            buffer.put(vbList);
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
            e.printStackTrace();
            throw e;
        }

    }

	private <X> X popNextOrNull(Iterator<X> p) {
		return p.hasNext() ? p.next() : null;
	}

//	public static void main(String[] args) {
		/* vBNumber number
	    int a;
		Scanner sn = new Scanner(System.in);
		a = sn.nextInt();
		VBIndex index = new VBIndex();
		byte[] s = index.vBNumber(a);
		System.out.println("VBNUMBER LENGTH: " + s.length);
		for (byte t : s) {
			for (int i = 0; i < Byte.SIZE; i++) {
				int n = t & (1 << (7 - i));
				n = n >> (7 - i);
				System.out.print(n);
			}

		}
		System.out.println("...number... ");
		System.out.println(index.number(s, s.length));
		*/

/*		ArrayList<Integer> doctest = new ArrayList<>();
		for (int i = 0; i < 9; i++) {
            doctest.add(i);
        }
        doctest.add(12);
		VBIndex index = new VBIndex();
        byte [] s = index.gapEncode(doctest);
		System.out.println("......gapEncode......");

        for (byte t : s) {
            for (int i = 0; i < Byte.SIZE; i++) {
                int n = t & (1 << (7 - i));
                n = n >> (7 - i);
                System.out.print(n);
            }
            System.out.println();

        }
        System.out.println("......vBDecode......");
        ByteBuffer buffer = ByteBuffer.allocate(VBNUM_SIZE * doctest.size());
        buffer.put(s);
        buffer.flip();
        ArrayList<Integer> dec = (ArrayList)index.vBDecode(buffer, s.length);
        for (int a : dec) {
            System.out.println(a);
        }
        System.out.println("......gapDecode......");

        buffer.flip();
        long pos = 0;
        Pair<PostingList, Integer> results = index.gapDecode(s.length, buffer, 1);
        PostingList postingList = results.getFirst();
        System.out.println(postingList);
        System.out.println("position : " + results.getSecond());
        return;
        */

		/* readposting writeposting


		Map<Integer, Pair<Long, Integer>> postingDict = new TreeMap<Integer, Pair<Long, Integer>>();

		PostingList [] ps = new PostingList[5];
		int a = 128; // ensure number increase
		for (int i = 0; i < ps.length; i++) {
			ArrayList<Integer> l = new ArrayList<>();
			for (int j = 0; j < (i & 0x3f) + 1; j++) {
				a += new Random().nextInt(50);
				l.add(a);
			}
			PostingList p = new PostingList(i, l);
			ps[i] = p;
		}

		VBIndex index = new VBIndex();

		// write posting
		try {
			RandomAccessFile bfc = new RandomAccessFile(new File("test.dict"), "rw");
			FileChannel fc = bfc.getChannel();

			for (int i = 0; i < ps.length; i++) {
				postingDict.put(ps[i].getTermId(), new Pair<>(fc.position(), ps[i].getList().size()));
				index.writePosting(fc, ps[i]);
			}

			bfc.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// read posting
		PostingList[] p = new PostingList[postingDict.size()];
		try {
			RandomAccessFile get = new RandomAccessFile(new File("test.dict"), "rw");
			int i = 0;
			for (int termid : postingDict.keySet()){
				Long pos = postingDict.get(termid).getFirst();
				if (pos != null) {
					p[i++] = index.readPosting(get.getChannel().position(pos));
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("......test read write posting......");

		for (int i = 0; i < ps.length; i++) {
			System.out.println("...write...");
			System.out.println(ps[i]);
			System.out.println("...read...");
			if (p[i] != null) {
				System.out.println(p[i]);
			}
		}*/
//	}


}
