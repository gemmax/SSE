package preprocessor;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import util.Pair;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Created by kalexjune on 17/4/27.
 *
 * 一个是以原始网页为输入,建立Html的 Dom Tree;
 * 另一个是存储分好的网页块(在我们的系统中,每一个网 页块就叫做一个块)并记录块与块之间的组织架构。
 */
public class BlockTree {
    private final static int subjectLensThreshold = 1000;
    private ArrayList<PageBlock> blockPool = new ArrayList<PageBlock>();
    private ArrayList<Pair<Element, Element>> domTree = null; // <Node, ParentNode>

    public ArrayList<Pair<Element,Element>> getDomTree() {
        return domTree;
    }

    public ArrayList<PageBlock> getBlockPool() {
        for (PageBlock p : blockPool) {
            p.activePageBlock(blockPool);
        }
        // caution must be active first
        cleanBlockPool();
        return blockPool;
    }

    private void cleanBlockPool() {

        for (int i = 0; i < blockPool.size(); ) {
            PageBlock p = blockPool.get(i);
            if (p.getTokens().length() == 0) {
                blockPool.remove(p);
            } else {
                i++;
            }
        }
    }

    public Element getParentElement(Element node) {
        if (domTree == null) return null;
        for (Pair<Element, Element> relation : domTree) {
            if (relation.getFirst() == node) {
                return relation.getSecond();
            }
        }
        return null;
    }

    private void buildTree(Node node) {
        if (node.parentNode() != null && node.parentNode() instanceof Element) {
            domTree.add(new Pair<Element, Element>((Element)node, (Element)(node.parent())));
        } else {
            domTree.add(new Pair<Element, Element>((Element)node, null));
        }
        for (Node child : node.childNodes()) {
            if (child instanceof Element) {
                buildTree(child);
            }
        }
    }

    // todo UNDO 剔除 无用网页 通过文件大小阈值 worth considerate
    public void buildTreeFromFile(String path) throws IOException {

        domTree = new ArrayList<Pair<Element, Element>>();
        // todo charset     采用Jsoup.parse(String html)， 对于未排版好的html解析错误。
        Document document = Jsoup.parse(new File(path), "utf-8");
        buildTree(document);
    }

    public void addBlock(Element node) {
        PageBlock block = new PageBlock(node);
        blockPool.add(block);
    }

    /**
     * 最初的主题块大小来判断是否属于主题性网站
     * @provide block pool sorted by block decrease.
     * @after SubjectSimilarityAnalyser.identifySubjectBlock
     * @return
     */
    public boolean isSubjectWeb() {
        if (blockPool.isEmpty())
            return false;
        return blockPool.get(0).getTokens().length() > subjectLensThreshold;
    }



    public static void main(String[] args) throws IOException {
        /* test html tree
        BlockTree blockTree = new BlockTree();
        blockTree.buildTreeFromFile("src/test/resources/a.html");

        BlockRecognizer.recognizer(blockTree, null);
        System.out.print("[ ");
        for (Pair<Element, Element> pair: blockTree.getDomTree()) {

            System.out.println("{ " + pair.getFirst().nodeName() + " : " + (pair.getSecond() == null ? "null" : pair.getSecond().nodeName()) + " } ");
        }
        System.out.print("]");
        */
    }

    /* get blocks from a page
    public String test() {
        for (PageBlock block : blockPool) {
            System.out.print(block.getBlock().nodeName() + " ");
        }
        System.out.print("\n");

        String tokens = "";
        for (PageBlock block : blockPool) {
            tokens += block.getTokens() + "\n";
        }
        return tokens;
    }
    */

}
