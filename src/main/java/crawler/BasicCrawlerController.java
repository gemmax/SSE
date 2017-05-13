package crawler;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

import java.io.File;

/**
 * Created by kalexjune on 17/4/23.
 */
public class BasicCrawlerController {
    public final static String pagesFolder = "src" + File.separator + "main"
                                                + File.separator + "webData";

    public static final String pageSourcesPath = pagesFolder + File.separator + "pgdb";
    public static final String output = pagesFolder + File.separator + "output";
    static {
        File outdir = new File(output);
        if (outdir.exists() && !outdir.isDirectory()) {
            System.err.println("Invalid output directory: " + output);
        }

        if (!outdir.exists()) {
            if (!outdir.mkdirs()) {
                System.err.println("Create output directory failure");
            }
        }
    }

    public static void main(String[] args) throws Exception {

        int numberOfCrawlers = 15;

        CrawlConfig config = new CrawlConfig();
        config.setCrawlStorageFolder(pagesFolder);
        config.setFollowRedirects(true);
        config.setMaxDepthOfCrawling(3);

        /*
         * Instantiate the controller for this crawl.
         */
        PageFetcher pageFetcher = new PageFetcher(config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
        robotstxtConfig.setEnabled(false);
        CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);

        // 403 被禁了
        // controller.addSeed("http://www.404notfound.fr/");
        controller.addSeed("https://en.wikipedia.org/wiki/Philosophy");
        controller.addSeed("https://en.wikipedia.org/wiki/Knowledge");
        controller.addSeed("https://en.wikipedia.org/wiki/Reality");
        controller.addSeed("http://www.bbc.com/news");
        controller.addSeed("https://www.javacodegeeks.com/");
        controller.addSeed("https://news.ycombinator.com/");
        controller.start(BasicCrawler.class, numberOfCrawlers);

    }

}
