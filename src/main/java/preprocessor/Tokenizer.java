package preprocessor;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by kalexjune on 17/5/2.
 */
public class Tokenizer {

    public static String[] splitEn(String text, boolean useStopWd) {
        String[] iterms = text.toLowerCase().split(" ");
        if (useStopWd == true) {
            loadEnStopWds();
            ArrayList<String> itermset = new ArrayList<>();
            for (String iterm : iterms) {
                if (!enStopWords.contains(iterm) && (!iterm.equals(""))) {
                    itermset.add(iterm);
                }
            }
            String [] tokens = new String[itermset.size()];
            itermset.toArray(tokens);
            return tokens;
        }
        return iterms;
    }

    /**
     * replace ' " to nil string
     * replace another punctuation to space
     * @param tokens
     * @return
     */
    public static String cleanToken(String tokens) {
        String result = "";
        result = tokens.replaceAll("\'","");
        result = result.replaceAll("\"","");
        result = result.replaceAll("[^a-zA-Z0-9\'\"]", " ");
        result = result.replaceAll(" {2,}", " ");
        result = result.trim();
        return result;
    }



    private static final HashSet<String> enStopWords = new HashSet<>();
    private static void loadEnStopWds() {
        // load english stop words file
        String path = "src" + File.separator + "main" + File.separator + "resources" + File.separator + "EnStopWords";
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(path)));
            String stopword;
            while ((stopword = bufferedReader.readLine()) != null) {
                enStopWords.add(stopword);
            }
            bufferedReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    public static void main(String[] args) {
        String test = "as young as you are, you have the ability to conquer world.";
        System.out.println("case 1 :");
        String[] test1 = Tokenizer.splitEn(test, false);
        for (String i1 : test1) {
            System.out.print(i1 + " ");
        }
        System.out.print("\n");
        System.out.println("case 2 :");
        String[] test2 = Tokenizer.splitEn(test, true);

        for (String i2 : test2) {
            System.out.print(i2 + " ");
        }
        System.out.print("\n");
    }
    */
}
