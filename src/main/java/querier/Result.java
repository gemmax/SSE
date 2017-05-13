package querier;

import crawler.BasicCrawler;
import crawler.BasicCrawlerController;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import preprocessor.ContentExtractor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by kalexjune on 17/5/5.
 */
public class Result {
    private static final int contInfoLens = 200;
    private String title;
    private String content;
    private String url;
    private static final String path = "/Users/kalexjune/IdeaProjects/SSE/" + BasicCrawlerController.pageSourcesPath;
    private static final String contentPath = "/Users/kalexjune/IdeaProjects/SSE/" + ContentExtractor.contentData;

    public Result(String url, int docId) throws IOException {
        this.url = url;
        Document document = Jsoup.parse(new File(path + File.separator + BasicCrawler.getBlockId(docId) + File.separator + docId), "utf-8");
        title = document.title();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(contentPath + File.separator + BasicCrawler.getBlockId(docId) + File.separator + docId)));
        content = bufferedReader.readLine();
    }

    public String getTitle() {
        return title;
    }

    public String getContent(String[] queryTerms) {

        // todo 5/5
        /**
         *

         Exception in thread "main" java.lang.StringIndexOutOfBoundsException: String index out of range: 200
         at java.lang.String.substring(String.java:1963)
         at querier.Result.getContent(Result.java:27)
         at querier.Query.main(Query.java:59)
         at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
         at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
         at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
         at java.lang.reflect.Method.invoke(Method.java:497)
         at com.intellij.rt.execution.application.AppMain.main(AppMain.java:147)

         */
        String result = content.length() > contInfoLens ? content.substring(0,contInfoLens) : content;
        return result.replaceAll("\\s+", " ");
    }

    public String getUrl() {
        return url;
    }
}
