package preprocessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

/**
 * Created by kalexjune on 17/5/4.
 */
public class SubjectSimilarityAnalyser {
    private static final double similarityThreshold = 0.6;


    public static void identifySubjectBlock(BlockTree blockTree) {
        ArrayList<PageBlock> blockPool = blockTree.getBlockPool();
        blockPool.sort(new Comparator<PageBlock>() {
            public int compare(PageBlock o1, PageBlock o2) {
                // decrease order
                return o2.getTokens().length() - o1.getTokens().length();
            }
        });

        if (blockPool.size() > 0) {
            PageBlock subjectBlock = blockPool.get(0);
            subjectBlock.setSubjectBlock(true);
            for (int i = 1; i < blockPool.size(); i++) {
                PageBlock block = blockPool.get(i);
                boolean isSubjectBlock =  reduceSubjectBlock(block, subjectBlock);
                block.setSubjectBlock(isSubjectBlock);
            }
        }
    }

    /**
     * token为文档中单个词条，此处已去除停用词
     * @param block
     * @param subjectBlock
     * @return
     */
    private static boolean reduceSubjectBlock(PageBlock block, PageBlock subjectBlock) {
        int sameTokenCount = 0;
        int sumTokenCount = 0;
        double similarity = 0;
        HashMap<String, Integer> tokenCount = convert2Tokens(block);
        HashMap<String, Integer> subTokenCount = convert2Tokens(subjectBlock);

        String[] tokens = new String[tokenCount.keySet().size()];
        tokenCount.keySet().toArray(tokens);
        Arrays.sort(tokens);
        String[] subTokens = new String[subTokenCount.keySet().size()];
        subTokenCount.keySet().toArray(subTokens);
        Arrays.sort(subTokens);


        int p1 = 0, p2 = 0;
        while (p1 < tokens.length && p2 < subTokens.length) {
            String token = tokens[p1];
            String subToken = subTokens[p2];
            // "abc".compareTo("cdf") = -2
            int compare = token.compareTo(subToken);
            if (compare == 0) {
                int nums1 = tokenCount.get(token);
                int nums2 = subTokenCount.get(subToken);
                sameTokenCount += (nums1 < nums2 ? nums1 : nums2);
                p1++;
                p2++;
            } else if (compare < 0) {
                p1++;
            } else if (compare > 0) {
                p2++;
            }
        }

        for (Integer t : tokenCount.values()) {
            sumTokenCount += t;
        }

        /*
        System.out.println("block :\n" + block.getTokens());
        System.out.println("subject block :\n" + subjectBlock.getTokens());
        System.out.println("same : " + sameTokenCount + " block sum : " + sumTokenCount);
        */
        similarity = ((double)sameTokenCount) / sumTokenCount;
        // System.out.println("similarity : " + similarity + "\n");
        return similarity > similarityThreshold;
    }

    private static HashMap<String, Integer> convert2Tokens(PageBlock block) {
        String[] tokens = Tokenizer.splitEn(block.getTokens(), true);
        HashMap<String, Integer> result = new HashMap<>();
        for (String item : tokens) {
            if (result.containsKey(item)){

                result.put(item, result.get(item) + 1);
            } else {

                result.put(item, 1);
            }
        }

        return result;
    }


}
