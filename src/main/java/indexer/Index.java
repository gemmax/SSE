package indexer;

import crawler.BasicCrawlerController;
import preprocessor.ContentExtractor;
import util.Pair;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;

public class Index {

	//TODO 改成SPIMI算法 20170512

	// Term id -> (position in index file, doc frequency) dictionary
	private static Map<Integer, Pair<Long, Integer>> postingDict
		= new TreeMap<Integer, Pair<Long, Integer>>();
	// Doc name -> doc id dictionary
/*	private static Map<String, Integer> docDict
			= new TreeMap<String, Integer>();*/
	// Term -> term id dictionary
	private static Map<String, Integer> termDict
		= new TreeMap<String, Integer>();
	// Block queue
	private static LinkedList<File> blockQueue
		= new LinkedList<File>();
	/*// Document counter
	private static int docIdCounter = 0;*/
	// Caution docId,worldId cannot be 0 for gamma algorithm.
	// Total file counter
	private static int totalFileCount = 0;

	// Term counter
	private static int wordIdCounter = 0;
	// Index
	private static BaseIndex index = null;

	
	/* 
	 * Write a posting list to the given file 
	 * You should record the file position of this posting list
	 * so that you can read it back during retrieval
	 * 
	 * */
	private static void writePosting(FileChannel fc, PostingList posting)
			throws Throwable {
		/*
		 * TODO: Your code here yeah
		 *	 
		 */
		//if (blockQueue.size() <= 1) {
		if (blockQueue.isEmpty()) {
			postingDict.put(posting.getTermId(), new Pair<Long, Integer>(fc.position(), posting.getList().size()));
		}

		index.writePosting(fc, posting);
	}

	public static void main(String[] args) throws Throwable {
		/* Parse command line */
		/*if (args.length != 3) {
			System.err
					.println("Usage: java Index [Basic|VB|Gamma] data_dir output_dir");
			return;
		}*/

		if (args.length != 1) {
			System.err
					.println("Usage: java Index [Basic|VB|Gamma]");
			return;
		}

		/* Get index */
		String className = "indexer." + args[0] + "Index";
		try {
			Class<?> indexClass = Class.forName(className);
			index = (BaseIndex) indexClass.newInstance();
		} catch (Exception e) {
			System.err
					.println("Index method must be \"Basic\", \"VB\", or \"Gamma\"");
			throw new RuntimeException(e);
		}

		/* Get root directory */
		// String root = args[1];
		String root = ContentExtractor.contentData;
		File rootdir = new File(root);
		if (!rootdir.exists() || !rootdir.isDirectory()) {
			System.err.println("Invalid data directory: " + root);
			return;
		}

		/* Get output directory */
		// String output = args[2];
		String output = BasicCrawlerController.output;
		File outdir = new File(output);
		if (outdir.exists() && !outdir.isDirectory()) {
			System.err.println("Invalid output directory: " + output);
			return;
		}

		if (!outdir.exists()) {
			if (!outdir.mkdirs()) {
				System.err.println("Create output directory failure");
				return;
			}
		}

		/* A filter to get rid of all files starting with .*/
		FileFilter filter = new FileFilter() {
			public boolean accept(File pathname) {
				String name = pathname.getName();
				return !name.startsWith(".");
			}
		};

		/* BSBI indexing algorithm */
		File[] dirlist = rootdir.listFiles(filter);

		/* For each block */
		for (File block : dirlist) {

			handleBlockTokens(output, block, root, filter);
		}

		/* Required: output total number of files. */
		System.out.println(totalFileCount);

		margeIndex(output);

		/* Dump constructed index back into file system */
		File indexFile = blockQueue.removeFirst();
		indexFile.renameTo(new File(output, "corpus.index"));

		BufferedWriter termWriter = new BufferedWriter(new FileWriter(new File(
				output, "term.dict")));
		for (String term : termDict.keySet()) {
			termWriter.write(term + "\t" + termDict.get(term) + "\n");
		}
		termWriter.close();

		/*BufferedWriter docWriter = new BufferedWriter(new FileWriter(new File(
				output, "doc.dict")));
		for (String doc : docDict.keySet()) {
			docWriter.write(doc + "\t" + docDict.get(doc) + "\n");
		}
		docWriter.close();*/

		BufferedWriter postWriter = new BufferedWriter(new FileWriter(new File(
				output, "posting.dict")));
		for (Integer termId : postingDict.keySet()) {
			postWriter.write(termId + "\t" + postingDict.get(termId).getFirst()
					+ "\t" + postingDict.get(termId).getSecond() + "\n");
		}
		postWriter.close();
	}

	private static void handleBlockTokens(String output, File block, String root, FileFilter filter) throws Throwable {

		File blockFile = new File(output, block.getName());
		blockQueue.add(blockFile);

		File blockDir = new File(root, block.getName());
		File[] filelist = blockDir.listFiles(filter);
			/* Add */
		TreeMap<Integer, TreeSet<Integer>> blockMap = new TreeMap<Integer, TreeSet<Integer>>(); //end

			/* For each file */
		for (File file : filelist) {
			++totalFileCount;
			//String fileName = block.getName() + "/" + file.getName();

			// because gamma cannot identify 0.
			// docDict.put(fileName, ++docIdCounter);

			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;
			while ((line = reader.readLine()) != null) {
				String[] tokens = line.trim().split("\\s+");
				for (String token : tokens) {
						/*
						 * TODO: Your code here yeah
						 *       For each term, build up a list of
						 *       documents in which the term occurs
						 */
					if (!termDict.containsKey(token)) {
						termDict.put(token, ++wordIdCounter);
					}
					if (!blockMap.containsKey(termDict.get(token))) {
						blockMap.put(termDict.get(token), new TreeSet<Integer>());
					}
					// set ensures docId unique
					// blockMap.get(termDict.get(token)).add(docDict.get(fileName));
					blockMap.get(termDict.get(token)).add(Integer.valueOf(file.getName()));
				}
			}
			reader.close();
		}

			/* Sort and output */
		if (!blockFile.createNewFile()) {
			System.err.println("Create new block failure.");
			return;
		}

		RandomAccessFile bfc = new RandomAccessFile(blockFile, "rw");
		FileChannel bf = bfc.getChannel();
			/*
			 * TODO: Your code here yeah
			 *       Write all posting lists for all terms to file (bfc)
			 */
		for (int termId : blockMap.keySet()) {
			writePosting(bf, new PostingList(termId, new ArrayList<Integer>(blockMap.get(termId))));
		}

		bfc.close();
	}

	private static void margeIndex(String output) throws Throwable {
		/* Merge blocks */
		while (true) {
			if (blockQueue.size() <= 1)
				break;

			File b1 = blockQueue.removeFirst();
			File b2 = blockQueue.removeFirst();
			File combfile = new File(output, b1.getName()+ "+" + b2.getName());
			if (!combfile.createNewFile()) {
				System.err.println("Create new block failure.");
				return;
			}

			RandomAccessFile bf1 = new RandomAccessFile(b1, "r");
			RandomAccessFile bf2 = new RandomAccessFile(b2, "r");
			RandomAccessFile mf = new RandomAccessFile(combfile, "rw");
			FileChannel b1f = bf1.getChannel();
			FileChannel b2f = bf2.getChannel();
			FileChannel mfc = mf.getChannel();
			/*
			 * TODO: Your code here yeah
			 *       Combine blocks bf1 and bf2 into our combined file, mf
			 *       You will want to consider in what order to merge
			 *       the two blocks (based on term ID, perhaps?).
			 *
			 */
			PostingList p1 = index.readPosting(b1f), p2 = index.readPosting(b2f);
			while (p1 != null || p2!= null) {
				if (p1 != null && (p2 == null || p1.getTermId() < p2.getTermId())){
					writePosting(mfc, p1);
					p1 = index.readPosting(b1f);

				}
				else if (p2 != null && (p1 == null || p2.getTermId() < p1.getTermId())){
					writePosting(mfc, p2);
					p2 = index.readPosting(b2f);
				}
				else if (p1 != null && p2 != null){
					writePosting(mfc,mergePosting(p1,p2));
					p1 = index.readPosting(b1f);
					p2 = index.readPosting(b2f);
				}
			}

			bf1.close();
			bf2.close();
			mf.close();
			b1.delete();
			b2.delete();
			blockQueue.add(combfile);
		}
	}

	private static PostingList mergePosting(PostingList p1, PostingList p2) {
		if (p1.getList().isEmpty()) return p2;
		if (p2.getList().isEmpty()) return p1;

		Iterator<Integer> pl1 = p1.getList().iterator();
		Iterator<Integer> pl2 = p2.getList().iterator();
        Integer doc1 = popNextOrNull(pl1);
        Integer doc2 = popNextOrNull(pl2);
		PostingList mergelist = new PostingList(p1.getTermId());

		while (doc1 != null || doc2 != null) {
			if (doc2 == null || (doc1 != null && doc1 < doc2)) {
			    mergelist.getList().add(doc1);
			    doc1 = popNextOrNull(pl1);
            } else if (doc1 == null || (doc2 != null && doc1 > doc2)) {
			    mergelist.getList().add(doc2);
			    doc2 = popNextOrNull(pl2);
            } else {
			    mergelist.getList().add(doc1);
			    doc1 = popNextOrNull(pl1);
			    doc2 = popNextOrNull(pl2);
            }

		}

		return mergelist;
	}

    static <X> X popNextOrNull(Iterator<X> p) {
        return p.hasNext() ? p.next() : null;
    }
}
