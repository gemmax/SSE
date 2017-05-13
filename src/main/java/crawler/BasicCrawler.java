package crawler;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Created by kalexjune on 17/4/23.
 * 网页相关处理数据位于 pagesFolder = src/main/webData
 * 抓取网页，存储在 pagesFolder/pgdb
 * 将url与docId的映射表存储在 pagesFolder/output/doc.dict
 *
 */
public class BasicCrawler extends WebCrawler {
    private static Object mutex = new Object();
    private static final Pattern IMAGE_EXTENSIONS = Pattern.compile(".*\\.(bmp|gif|jpg|png)$");
    private static final int blockSize = 500;
    private static final long startTime = System.currentTimeMillis();

    // Document counter
    private static int docIdCounter = 0;
    private static int blockId = 0;
    private static int blockfilecounter = 0;

    // write Doc name (web url) -> doc id dictionary
    private static BufferedWriter docWriter;

    static {
        try {

            /* Caution
            因为 FileOutputStream 对象被创建时会执行一个 native 的 open() 操作，如果没有指定 append 属性为 true，则指针会移动到文件开始的位置，相当于清空了文件操作。
            所以，使用 new FileWriter(file, true),
            而不是 FileWriter(file)
             */
            docWriter = new BufferedWriter(new FileWriter(new File(
                    BasicCrawlerController.output, "doc.dict"),true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * You should implement this function to specify whether the given url
     * should be crawled or not (based on your crawling logic).
     */
    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String href = url.getURL().toLowerCase();
        // Ignore the url if it has an extension that matches our defined set of image extensions.
        if (IMAGE_EXTENSIONS.matcher(href).matches()) {
            return false;
        }

        // Only accept the url if it is in the "www.ics.uci.edu" domain and protocol is "http".
        // return href.startsWith("http://www.ics.uci.edu/");
        return true;
    }


    @Override
    public void visit(Page page) {
        String url = page.getWebURL().getURL();
        if (page.getParseData() instanceof HtmlParseData) {
            HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
            String html = htmlParseData.getHtml();
            // System.out.println(html);
            // Document doc = Jsoup.parse(html);

            try {
                writeToDisk(page.getWebURL(), html);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void writeToDisk(WebURL webURL, String html) throws IOException {
        synchronized (mutex) {
            /*
            for (String doc : docDict.keySet()) {
			    docWriter.write(doc + "\t" + docDict.get(doc) + "\n");
		    }
             */
            docWriter.append(webURL.getURL() + "\t" + (++docIdCounter) + "\n");
            docWriter.flush();
            blockfilecounter++;

            File file = new File(BasicCrawlerController.pageSourcesPath + File.separator + blockId);
            // System.out.println("block id : " + blockId);
            if (!file.exists()) {
                file.mkdirs();
            }
            if (blockfilecounter % blockSize == 0) {
                blockId++;
            }
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(file, "" + docIdCounter)));
            // bw.write("docId : " + webURL.getDocid() + "\nurl : " + webURL.getURL() + "\n");
            bw.write(html);
            bw.flush();
            bw.close();
        }
    }

    public static int getBlockId(int docId) {
        return (docId - 1) / blockSize;
    }

}
