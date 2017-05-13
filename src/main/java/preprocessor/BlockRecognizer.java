package preprocessor;

import org.jsoup.nodes.Element;
import util.Pair;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by kalexjune on 17/4/27.
 * 肩负网页分块的重任,从网页中识别出所有语义块。它依赖于前面的两个类。
 */
public class BlockRecognizer {
    private final static HashMap<String, String> identifyBlock = new HashMap<String, String>();

    private final static String TAG_S = "s";
    private final static String TAG_B = "b";
    private final static String TAG_L = "l";
    private final static String TAG_D = "d";
    private final static String TAG_A = "a";
    private final static String TAG_C = "c";
    private final static int textThreshold = 20; // todo byte?  ensure len threshold
    private final static int nodeNumThreshold = 3; // todo ensure the threshold of the number of l tag's node.

    // if tag is L, D or A; and text contains regular expression. Then identify the tag C.S
    static {
        identifyBlock.put("head", TAG_S);
        identifyBlock.put("script", TAG_S);
        identifyBlock.put("style", TAG_S);
        identifyBlock.put("object", TAG_S);
        identifyBlock.put("frameset", TAG_S);
        identifyBlock.put("iframe", TAG_S);
        identifyBlock.put("html", TAG_S);

        identifyBlock.put("div", TAG_B);
        identifyBlock.put("td", TAG_B);
        identifyBlock.put("table", TAG_B);
        identifyBlock.put("form", TAG_B);
        identifyBlock.put("fieldset", TAG_B);
        identifyBlock.put("center", TAG_B);
        identifyBlock.put("noframes", TAG_B);
        identifyBlock.put("noscript", TAG_B);
        identifyBlock.put("pre", TAG_B);
        identifyBlock.put("body", TAG_B);
        // identifyBlock.put("html", TAG_B);

        identifyBlock.put("p", TAG_L);
        identifyBlock.put("ul", TAG_L);
        identifyBlock.put("ol", TAG_L);
        identifyBlock.put("dl", TAG_L);
        identifyBlock.put("dir", TAG_L);
        identifyBlock.put("li", TAG_L);
        identifyBlock.put("dt", TAG_L);
        identifyBlock.put("blockquote", TAG_L);
        identifyBlock.put("address", TAG_L);
        identifyBlock.put("br", TAG_L);
        identifyBlock.put("wbr", TAG_L);
        identifyBlock.put("hr", TAG_L);
        identifyBlock.put("col", TAG_L);
        identifyBlock.put("colgroup", TAG_L);
        identifyBlock.put("img", TAG_L);
        identifyBlock.put("menu", TAG_L);
        identifyBlock.put("select", TAG_L);

        identifyBlock.put("a", TAG_D);
        identifyBlock.put("abbr", TAG_D);
        identifyBlock.put("acronym", TAG_D);
        identifyBlock.put("area", TAG_D);
        identifyBlock.put("b", TAG_D);
        identifyBlock.put("blod", TAG_D);
        identifyBlock.put("base", TAG_D);
        identifyBlock.put("basefont", TAG_D);
        identifyBlock.put("bdo", TAG_D);
        identifyBlock.put("big", TAG_D);
        identifyBlock.put("button", TAG_D);
        identifyBlock.put("caption", TAG_D);
        identifyBlock.put("cite", TAG_D);
        identifyBlock.put("code", TAG_D);
        identifyBlock.put("dd", TAG_D);
        identifyBlock.put("del", TAG_D);
        identifyBlock.put("dfn", TAG_D);
        identifyBlock.put("em", TAG_D);
        identifyBlock.put("font", TAG_D);
        identifyBlock.put("h1", TAG_D);
        identifyBlock.put("h2", TAG_D);
        identifyBlock.put("h3", TAG_D);
        identifyBlock.put("h4", TAG_D);
        identifyBlock.put("h5", TAG_D);
        identifyBlock.put("h6", TAG_D);
        identifyBlock.put("i", TAG_D);
        identifyBlock.put("ins", TAG_D);
        identifyBlock.put("kbd", TAG_D);
        identifyBlock.put("label", TAG_D);
        identifyBlock.put("small", TAG_D);
        identifyBlock.put("strike", TAG_D);
        identifyBlock.put("strong", TAG_D);
        identifyBlock.put("sub", TAG_D);
        identifyBlock.put("sup", TAG_D);
        identifyBlock.put("q", TAG_D);
        identifyBlock.put("s", TAG_D);
        identifyBlock.put("samp", TAG_D);
        identifyBlock.put("span", TAG_D);
        identifyBlock.put("thead", TAG_D);
        identifyBlock.put("tfoot", TAG_D);
        identifyBlock.put("textarea", TAG_D);
        identifyBlock.put("u", TAG_D);
        identifyBlock.put("tt", TAG_D);
        identifyBlock.put("var", TAG_D);
        identifyBlock.put("o:smarttagtype", TAG_D);

        identifyBlock.put("frame", TAG_A);
        identifyBlock.put("input", TAG_A);
        identifyBlock.put("isindex", TAG_A);
        identifyBlock.put("legend", TAG_A);
        identifyBlock.put("link", TAG_A);
        identifyBlock.put("map", TAG_A);
        identifyBlock.put("meta", TAG_A);
        identifyBlock.put("option", TAG_A);
        identifyBlock.put("optgroup", TAG_A);
        identifyBlock.put("param", TAG_A);
        identifyBlock.put("td", TAG_A);
        identifyBlock.put("th", TAG_A);
        identifyBlock.put("tr", TAG_A);
        identifyBlock.put("tbody", TAG_A);
        identifyBlock.put("title", TAG_A);
    }

    /**
     *
     * @param blockTree a text block tree
     * @param regexs a list of regular expression, aim at identify tag C.
     */
    public static void recognizer(BlockTree blockTree, String[] regexs) {
        LinkedList<Element> textQueue = new LinkedList<Element>();
        ArrayList<Pair<Element, Element>> domTree = blockTree.getDomTree();
        HashMap<Element, Integer> rNodeNums = new HashMap<Element, Integer>();

        if (domTree != null) {
            initTextQueue(domTree, textQueue);
            // todo 取textQueue第一个节点，判断是否加入网页池，循环遍历，直至队列为空
            int index = 0;
            while (index < textQueue.size()) {
                add2Block(blockTree, regexs, index, rNodeNums, textQueue);
                index++;
            }
        }
    }

    /**
     * {@code test ContentExtractor.main()}
     * @param blockTree
     * @param regexs
     * @param indexCpy
     * @param bNodes
     * @param textQueue
     */
    private static void add2Block(BlockTree blockTree, String[] regexs, int indexCpy, HashMap<Element, Integer> bNodes, LinkedList<Element> textQueue) {
        Element node = textQueue.get(indexCpy);
        String idtTag = identifyTag(node, regexs);
        if (idtTag.equals(TAG_S) || idtTag.equals(TAG_C)) {
            blockTree.addBlock(node);
        }
        if (idtTag.equals(TAG_D) || idtTag.equals(TAG_A)) {
            Element superNode = blockTree.getParentElement(node);
            if (!textQueue.contains(superNode)) {
                textQueue.add(superNode);
            }
        }
        if (idtTag.equals(TAG_L)) {
            Element superNode = blockTree.getParentElement(node);
            int nodeNums = 0;
            if (bNodes.containsKey(superNode)) {
                nodeNums = bNodes.get(superNode);
            }
            nodeNums++;
            bNodes.put(superNode, nodeNums);

            if (!textQueue.contains(superNode)) {
                textQueue.add(superNode);
            }
        }
        if (idtTag.equals(TAG_B)) {
            int print = PageBlock.getNodeTokens(node, blockTree.getBlockPool());
            if (print >= textThreshold
                    || (bNodes.containsKey(node)
                            && bNodes.get(node) >= nodeNumThreshold)) {
                blockTree.addBlock(node);
            } else {
                Element superNode = blockTree.getParentElement(node);
                int nodeNums = 0;
                if (bNodes.containsKey(superNode)) {
                    nodeNums = bNodes.get(superNode);
                }
                nodeNums++;
                bNodes.put(superNode, nodeNums);

                if (!textQueue.contains(superNode)) {
                    textQueue.add(superNode);
                }
            }
        }
    }

    private static String identifyTag(Element node, String[] regexs) {
        String identifyTag = identifyBlock.get(node.tagName());
        // todo 遇到未标识的标签，如<time>, 默认归置Display Tag. done
        if (identifyTag == null) {
            identifyTag = TAG_D;
        }
        if (regexs != null &&
                (identifyTag.equals(TAG_D) || identifyTag.equals(TAG_A) || identifyTag.equals(TAG_L))) {
            String content = node.text();
            Boolean isMatch = false;
            for (String regex : regexs) {
                regex = ".*" + regex + ".*";
                isMatch = Pattern.matches(regex, content);
                if (isMatch == true) {
                    return TAG_C;
                }
            }
        }
        return identifyTag;
    }

    private static void initTextQueue(ArrayList<Pair<Element, Element>> node, LinkedList<Element> textQueue) {
        HashSet<Element> parents = new HashSet<Element>();
        Element[] sets = new Element[node.size()];
        for (int i = 0; i < node.size(); i++) {
            sets[i] = node.get(i).getFirst();
            if (node.get(i).getSecond() != null) {
                parents.add(node.get(i).getSecond());
            }
        }
        for (Element element : sets) {
            if (!parents.contains(element)) {
                textQueue.add(element);
            }
        }
    }

    /* todo test BlockRecognizer.class
    private static void extractTokenStream(File page) throws IOException {
         test add to block {@code BlockRecognizer.add2Block()}
        // File tokens = new File("src/test/resources/testblockqueue.token"); // todo test blockqueue.html
        // File tokens = new File("src/test/resources/testComplexBlockRec.token"); // todo test blockrecognizer.html
        // File tokens = new File(contentData, page.getName() + ".tokens");
        BufferedWriter writer = new BufferedWriter(new FileWriter(tokens));

        // todo 30
        BlockTree blockTree = new BlockTree();
        blockTree.buildTreeFromFile(page.getPath());
        BlockRecognizer recognizer = new BlockRecognizer();
        recognizer.recognizer(blockTree, null);
        String test = blockTree.test();

        writer.write(test);
        writer.flush();
        writer.close();

        // page.delete();

    }*/

    public static void main(String[] args) throws IOException {
        /* todo test BlockRecognizer.class
        File[] list = new File(BasicCrawlerController.pageSourcesPath).listFiles();
        for (File page : list) {
            extractTokenStream(page);
        }
        //extractTokenStream(new File("src/test/resources/TestBlockQueue.html")); // todo test blockqueue.html
        //extractTokenStream(new File("src/test/resources/TestComplexBlockRec.html")); // todo test blockrecognizer.html
        */

    }
}
