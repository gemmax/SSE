package preprocessor;

import crawler.BasicCrawlerController;
import org.jsoup.nodes.Element;
import util.Pair;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by kalexjune on 17/4/27.
 * 演示类,用来查看Quark模块各步骤的实现效果。 目前可以查看网页分块的效果。
 */
public class BlockHtmlBuilder {
    private static final String blockTestPath = "src" + File.separator + "test" + File.separator
                                                + "resources" + File.separator + "blockTest";
    private static final String[] colorset = {"black", "Aqua", "Aquamarine", "Blue", "BlueViolet", "Brown", "CadetBlue", "CornflowerBlue", "Fuchsia", "Red"};

    static {
        File outdir = new File(blockTestPath);
        if (outdir.exists() && !outdir.isDirectory()) {
            System.err.println("Invalid output directory: " + blockTestPath);
        }

        if (!outdir.exists()) {
            if (!outdir.mkdirs()) {
                System.err.println("Create output directory failure");
            }
        }
    }

    public static void main(String[] args) throws IOException {
        String sourcePath = BasicCrawlerController.pageSourcesPath;
        File[] files = new File(sourcePath).listFiles();
        for (File page : files) {
            File test = new File(blockTestPath, page.getName() + ".html");
            BufferedWriter writer = new BufferedWriter(new FileWriter(test));
            // todo 30
            BlockTree blockTree = new BlockTree();
            blockTree.buildTreeFromFile(page.getPath());
            BlockRecognizer recognizer = new BlockRecognizer();
            recognizer.recognizer(blockTree, null);
            String blockedHtml = htmlBuild(blockTree);
            writer.write(blockedHtml);
            writer.flush();
            writer.close();

        }
    }

    private static String htmlBuild(BlockTree blockTree) {
        ArrayList<Pair<Element, Element>> domTree = blockTree.getDomTree();
        ArrayList<PageBlock> blockPool = blockTree.getBlockPool();
        int colorIdx = 0; // 0-9
        for (Pair<Element, Element> relation : domTree) {
            Element node = relation.getFirst();
            for (PageBlock block : blockPool) {
                if (block.relate(node)) {
                    colorIdx %= 10;
                    node.wrap("<font color=\"" + colorset[colorIdx] + "\"></font>");
                    colorIdx++;
                }
            }
        }
        return domTree.get(0).getFirst().html();
    }

}
