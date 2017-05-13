package preprocessor;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.ArrayList;

/**
 * Created by kalexjune on 17/4/27.
 * 保存单个网页块的信息
 */
public class PageBlock {

    private Element block;

    private String tokens = "";

    private boolean isSubjectBlock = false;

    public boolean isSubjectBlock() {
        return isSubjectBlock;
    }

    public void setSubjectBlock(boolean subjectBlock) {
        isSubjectBlock = subjectBlock;
    }

    public PageBlock(Element block) {
        this.block = block;
    }

    public Element getBlock() {
        return block;
    }

    public void setBlock(Element block) {
        this.block = block;
    }

    public void activePageBlock(ArrayList<PageBlock> blockPool) {
        setTokens(blockPool);
    }

    public boolean relate(Element node) {
        if (block == node)
            return true;
        return false;
    }

    /**
     * 由于token流无需考虑语序，所以此处语序与原语序不同
     * @param blockPool
     */
    private void setTokens(ArrayList<PageBlock> blockPool) {
        tokens = token(block, blockPool);
        tokens = Tokenizer.cleanToken(tokens);
    }

    public String getTokens() {
        return tokens;
    }

    private static String token(Element node, ArrayList<PageBlock> blockPool) {
        String result = node.ownText();
        for (Node it : node.children()) {
            if (it instanceof Element) {
                boolean isBlock = false;
                for (PageBlock p : blockPool) {
                    if (p.relate((Element) it)) {
                        isBlock = true;
                        break;
                    }
                }
                if (!isBlock) {
                    result += " " + token((Element) it, blockPool);
                }
            }
        }
        return result;

    }


    /**
     * assume that node is in PageBlock, but the token stream of this node won't be saved.
     * @param blockPool
     * @return node text length without the text length of block child.
     */
    public static int getNodeTokens(Element node, ArrayList<PageBlock> blockPool) {

        String tokens = "";
        tokens = token(node, blockPool);
        tokens = Tokenizer.cleanToken(tokens);

        int lens = tokens.length();
        return lens;
    }


    public static void main(String[] args) {
        /* test cleanToken ok
        PageBlock pageBlock = new PageBlock(null);
        String a = "  this is   a  sip@and .overwhe\'lm\"ing with  what     i do.";
        String b = pageBlock.cleanToken(a);
        System.out.println(a);
        System.out.println(b);
        String c = "thisisasip@and.overwhe\'lm\"ingwithwhatido.";
        String d = pageBlock.cleanToken(a);
        System.out.println(c);
        System.out.println(d);
        */
    }

}
