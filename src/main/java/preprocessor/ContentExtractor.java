package preprocessor;

import crawler.BasicCrawlerController;

import java.io.*;
import java.util.*;

/**
 * Created by kalexjune on 17/4/25.
 * 它在分好的块的基础上, 判断各个块的类型,提取正文信息。
 * 基于文本相似度
 */
public class ContentExtractor {
    public final static String contentData = BasicCrawlerController.pagesFolder + File.separator + "contentData";

    static {
        File outdir = new File(contentData);
        if (outdir.exists() && !outdir.isDirectory()) {
            System.err.println("Invalid output directory: " + contentData);
        }

        if (!outdir.exists()) {
            if (!outdir.mkdirs()) {
                System.err.println("Create output directory failure");
            }
        }
    }


    private static boolean isSubjectWebPage(BlockTree blockTree) {

        return blockTree.isSubjectWeb();
    }

    public static void main(String[] args) throws IOException {
        /**
         * ContentExtractor.class
         * test identify subject web page and non-subject web page
        String testFiles = "src/test/resources";
        String savePath = "src/test/resources/tokens/";
        File dir = new File(testFiles);
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                continue;
            }
            String fpath = f.getPath();
            String saveName;
            if ((f.getName().split(".")).length > 0) {
                saveName = savePath + (f.getName().split("."))[0] + ".tokens";
            } else {
                saveName = savePath + f.getName() + ".tokens";
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(saveName)));

            BlockTree blockTree = new BlockTree();
            blockTree.buildTreeFromFile(fpath);

            String subjectToken = ContentExtractor.extractSubjectByFile(blockTree);
            // caution firstly must identify similarity web page
            if (ContentExtractor.isSubjectWebPage(blockTree)) {
                String s = "subject web page\n";
                writer.write(s);
                writer.write(subjectToken);
            } else {
                String s = "not subject web page\n";
                writer.write(s);
                String token = ContentExtractor.extractWholeByFile(blockTree);
                writer.write(token);
            }
            writer.flush();
            writer.close();
        }
        */
        /* A filter to get rid of all files starting with .*/
        FileFilter filter = new FileFilter() {
            public boolean accept(File pathname) {
                String name = pathname.getName();
                return !name.startsWith(".");
            }
        };
        File pageFiles = new File(BasicCrawlerController.pageSourcesPath);
        for (File block : pageFiles.listFiles(filter)) {

            File blockToken = new File(contentData, block.getName());
            if (!blockToken.exists()) {
                blockToken.mkdir();
            }

            for (File file : block.listFiles(filter)) {
                writeTokens(file, blockToken.getPath());
            }
        }
    }

    private static void writeTokens(File file, String parentPath) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(parentPath, file.getName())));
        String tokens = ContentExtractor.extractTokens(file.getPath());
        writer.write(tokens);
        writer.flush();
        writer.close();
    }

    private static String extractSubjectByFile(BlockTree blockTree) throws IOException {

        BlockRecognizer.recognizer(blockTree, null);
        SubjectSimilarityAnalyser.identifySubjectBlock(blockTree);

        String tokens = "";
        for (PageBlock p : blockTree.getBlockPool()) {
            if (p.isSubjectBlock()) {
                tokens += " " + p.getTokens();
            }
        }
        tokens = tokens.trim();
        return tokens;
    }

    private static String extractWholeByFile(BlockTree blockTree) throws IOException {
        String tokens = blockTree.getDomTree().get(0).getFirst().text();
        tokens = Tokenizer.cleanToken(tokens);
        return tokens;
    }

    public static String extractTokens(String filepath) throws IOException {
        if (new File(filepath).isDirectory()) {
            return null;
        }

        BlockTree blockTree = new BlockTree();
        blockTree.buildTreeFromFile(filepath);

        String tokens = ContentExtractor.extractSubjectByFile(blockTree);
        // caution firstly must identify similarity web page
        if (!ContentExtractor.isSubjectWebPage(blockTree)) {
            tokens = ContentExtractor.extractWholeByFile(blockTree);
        }
        return tokens;
    }

}
