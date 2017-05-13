package preprocessor;

import org.jsoup.nodes.Element;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by kalexjune on 17/4/27.
 * 17/5/3
 * 评测类,用来评测Quark核心类的实现效果。当前 实现的是对网页正文信息提取的评测,
 * 评测需要接受人工标记的网页或 网页集为输入。评测算法的细节见后文。
 */
public class BlockEvaluation {

    private static final String[] colorset = {"Aqua", "Aquamarine", "BlueViolet", "Brown", "CadetBlue", "CornflowerBlue", "Fuchsia", "Red"};
    public static void main(String[] args) throws IOException {
        String testFile = "src/test/resources/a.html";
        String resultFile = "src/test/resources/resultA.html";
        BlockTree blockTree = new BlockTree();
        blockTree.buildTreeFromFile(testFile);

        BlockRecognizer.recognizer(blockTree, null);
        SubjectSimilarityAnalyser.identifySubjectBlock(blockTree);

        String html = buildSubjectWebBlock(blockTree);
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(resultFile)));
        bufferedWriter.write(html);
        bufferedWriter.flush();
        bufferedWriter.close();

    }

    private static String buildSubjectWebBlock(BlockTree blockTree) {
        ArrayList<PageBlock> blockPool = blockTree.getBlockPool();
        int colorIdx = 0;
        for (PageBlock block : blockPool) {
            Element p = block.getBlock();
            System.out.println(p.nodeName() + " -> " + blockTree.getParentElement(p).nodeName());
            System.out.println(block.getBlock().nodeName() + " : " + (block.isSubjectBlock() ? "is ":"isn\'t ") + "subject block");
            System.out.println(block.getTokens());
            System.out.println("len : " + block.getTokens().length());

            System.out.println();
            if (block.isSubjectBlock()) {
                colorIdx %= 10;
                block.getBlock().wrap("<font color=\"" + colorset[colorIdx] + "\"></font>");
                colorIdx++;
            }
        }
        return blockTree.getDomTree().get(0).getFirst().html();
    }
}
