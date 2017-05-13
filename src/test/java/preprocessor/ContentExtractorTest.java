package preprocessor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by kalexjune on 17/5/4.
 */
public class ContentExtractorTest {

    public static void main(String[] args) throws IOException {
         /* test ContentExtractor.extractTokens(String filepath) */
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

            String s = ContentExtractor.extractTokens(fpath);

            writer.write(s);
            writer.flush();
            writer.close();
        }
    }
}
