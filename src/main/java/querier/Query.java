package querier;

import crawler.BasicCrawlerController;
import indexer.BaseIndex;
import indexer.PostingList;
import preprocessor.PageBlock;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;

public class Query {

	// Term id -> position in index file
	private static Map<Integer, Long> posDict = new TreeMap<Integer, Long>();
	// Term id -> document frequency
	private static Map<Integer, Integer> freqDict = new TreeMap<Integer, Integer>();
	// Doc id -> doc name dictionary
	private static Map<Integer, String> docDict = new TreeMap<Integer, String>();
	// Term -> term id dictionary
	private static Map<String, Integer> termDict = new TreeMap<String, Integer>();
	// Index
	private static BaseIndex index = null;

	
	/* 
	 * Write a posting list with a given termID from the file 
	 * You should seek to the file position of this specific
	 * posting list and read it back.
	 * */
	private static PostingList readPosting(FileChannel fc, int termId)
			throws Throwable {
		/*
		 * TODO: Your code here yeah
		 */
		Long position = posDict.get(termId);
		if (position != null) {
			PostingList p = index.readPosting(fc.position(position));
			return p;
		}
		return null;
	}

	public static void main(String[] args) throws Throwable {
		/* Parse command line */
		/*if (args.length != 2) {
			System.err.println("Usage: java Query [Basic|VB|Gamma] index_dir");
			return;
		}*/
		if (args.length != 1) {
			System.err.println("Usage: java Query [Basic|VB|Gamma]");
			return;
		}
		// todo
		Result [] results = Query.query("quora");
		String[] terms = "quora".split("\\s+");
		for (Result result : results) {
			System.out.println(result.getTitle());
			System.out.println(result.getContent(terms));
			System.out.println(result.getUrl());
		}
	}

	private static List<Integer> intersect(List<Integer> list, List<Integer> temp) {
		if (temp == null) return list;

		List<Integer> answer = new ArrayList<Integer>();
		Iterator<Integer> pl = list.iterator();
		Iterator<Integer> pt = temp.iterator();
		Integer doc1 = popNextOrNull(pl);
		Integer doc2 = popNextOrNull(pt);

		while (doc1 != null && doc2 != null) {
			if (doc1 < doc2) {
				doc1 = popNextOrNull(pl);
			} else if (doc1 > doc2) {
				doc2 = popNextOrNull(pt);
			} else {
				answer.add(doc1);
				doc1 = popNextOrNull(pl);
				doc2 = popNextOrNull(pt);
			}
		}
		if (answer.isEmpty()) return null;
		return answer;
	}

	static <X> X popNextOrNull(Iterator<X> p) {
		return p.hasNext() ? p.next() : null;
	}

	/**
	 * Use Gamma code
	 * todo change relative path to absolute path.
	 * @param query
	 * @return
	 * @throws Throwable
	 */
	public static Result[] query(String query) throws Throwable {
		/* Get index */
		// String className = "indexer." + args[0] + "Index";
		String className = "indexer.GammaIndex";
		try {
			Class<?> indexClass = Class.forName(className);
			index = (BaseIndex) indexClass.newInstance();
		} catch (Exception e) {
			System.err
					.println("Index method must be \"Basic\", \"VB\", or \"Gamma\"");
			throw new RuntimeException(e);
		}

		/* Get index directory */
		String input = "/Users/kalexjune/IdeaProjects/SSE/" + BasicCrawlerController.output;
		File inputdir = new File(input);
		if (!inputdir.exists() || !inputdir.isDirectory()) {
			System.err.println("Invalid index directory: " + input);
			return null;
		}

		/* Index file */
		RandomAccessFile indexFile = new RandomAccessFile(new File(input,
				"corpus.index"), "r");

		String line = null;
		/* Term dictionary */
		BufferedReader termReader = new BufferedReader(new FileReader(new File(
				input, "term.dict")));
		while ((line = termReader.readLine()) != null) {
			String[] tokens = line.split("\t");
			termDict.put(tokens[0], Integer.parseInt(tokens[1]));
		}
		termReader.close();

		/* Doc dictionary */
		BufferedReader docReader = new BufferedReader(new FileReader(new File(
				input, "doc.dict")));
		while ((line = docReader.readLine()) != null) {
			String[] tokens = line.split("\t");
			docDict.put(Integer.parseInt(tokens[1]), tokens[0]);
		}
		docReader.close();

		/* Posting dictionary */
		BufferedReader postReader = new BufferedReader(new FileReader(new File(
				input, "posting.dict")));
		while ((line = postReader.readLine()) != null) {
			String[] tokens = line.split("\t");
			posDict.put(Integer.parseInt(tokens[0]), Long.parseLong(tokens[1]));
			freqDict.put(Integer.parseInt(tokens[0]),
					Integer.parseInt(tokens[2]));
		}
		postReader.close();

		/* Processing queries */
		// BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		/* For each query */
		//while ((line = br.readLine()) != null) {
			/*
			 * TODO: Your code here yeah
			 *       Perform query processing with the inverted index.
			 *       Make sure to print to stdout the list of documents
			 *       containing the query terms, one document file on each
			 *       line, sorted in lexicographical order.
			 */
			// get query term sort by freq.
		String[] queryTerms = query.trim().split("\\s+");
		TreeSet<Integer> qtermIds = new TreeSet<Integer>(new Comparator<Integer>() {
			public int compare(Integer o1, Integer o2) {
				// increased sort noted!!!
				if (freqDict.get(o1) != null && freqDict.get(o2) != null){
					return freqDict.get(o1) - freqDict.get(o2);
				}
				return 0;
			}
		});
		for (String term : queryTerms) {
			if (termDict.get(term) != null) {
				qtermIds.add(termDict.get(term));
			}
		}

		// combine two postinglist and once intersect read once file
		List<Integer> temp = null;
		while (!qtermIds.isEmpty()) {
			int term = qtermIds.first();
			qtermIds.remove(term);
			PostingList p = readPosting(indexFile.getChannel(), term);
			if ( p!= null && p.getList() != null) {
				temp = intersect(p.getList(), temp);
			}
		}

		/*String[] results;
		if (temp == null) {
			// results  = new String[]{"no result found"};
			return null;
		} else {
			results = new String[temp.size()];
			int f = 0;
			for (int docId : temp) {
				results[f++] = docDict.get(docId);
			}
			Arrays.sort(results);
			for (String result: results) {
				System.out.println(result);
			}
		}*/

		Result[] results = null;
		if (temp != null) {
			results = new Result[temp.size()];
			for (int i = 0; i < temp.size(); i++) {
				Result result = new Result(docDict.get(temp.get(i)), temp.get(i));
				results[i] = result;
			}
		}

		indexFile.close();
		return results;


		// }
		// br.close();
	}
}
