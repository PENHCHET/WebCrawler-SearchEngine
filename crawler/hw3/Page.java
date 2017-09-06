package hw3;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.annotations.Expose;
import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.select.Elements;
import org.omg.Messaging.SYNC_WITH_TRANSPORT;
import org.tartarus.snowball.ext.PorterStemmer;
import our.canonicalizer.Canonicalizer;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * an id, the URL, the HTTP HTTP header, the page contents cleaned (with term positions), the raw html,
 * and a list of all in-links (known) and out-links for the page.
 * Logs crawling result into log file.
 */

public class Page {
    @Expose
    private String docno;
    @Expose
    private String url;
    @Expose
    private String title;
    @Expose
    private Map<String, String> HTTPheader;
    @Expose
    private String text;
    @Expose
    private String html_Source;
    @Expose
    private int depth;
    private Set<String> inLinks;
    private Set<String> outLinks;
    private Map<String, QueElement> outElements;
    private StringBuilder log;
    private RobotReader robotReader;
    private List<String> forbidenUrl;
    private Document document;
    private String domain;

    public Page(String url,
                int waveNum,
                Set<String> inLinks,
                String logFilePath,
                RobotReader robotReader,
                List<String> forbidenUrl) {
        if (url == null || url.length() == 0) {
            log.append("\nWeb Url not correct.\n");
            throw new IllegalArgumentException("not correct url");
        }
        this.depth = waveNum;
        this.url = url;
        this.forbidenUrl = forbidenUrl;
        this.robotReader = robotReader;
        this.inLinks = new HashSet<>(inLinks);
        this.outElements = new HashMap<>();
        outLinks = new HashSet<>();
        log = new StringBuilder();
        File file = new File(logFilePath);
        if (!setup()) {
            log.append("\nPage Ignored:\n");
            writeLog(file);
            throw new IllegalArgumentException("Ignored Page, details in log");
        }
        parseLinks();
        writeLog(file);
    }

    private void writeLog(File file){
        try {
            FileUtils.writeStringToFile(file, log.toString(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean setup() {
        this.domain = getDomainName(url);
        log.append("\nnew page: " + url);
        try {
            this.docno = Canonicalizer.apply(url);
        } catch (Exception e) {
            log.append(e.getMessage());
            return false;
        }
        try {
            url = java.net.URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException ue) {
            log.append(ue.getMessage());
            ue.printStackTrace();
            return false;
        }
        try {
            Connection connection = Jsoup.connect(url).timeout(1000);
            Connection.Response response = connection.execute();
            this.HTTPheader = response.headers();
            this.document = connection.get();
        } catch (IOException ie) {
            log.append(ie.getMessage());
            return false;
        }
        if (!robotReader.run(domain, url)) {
            forbidenUrl.add(url);
            log.append("\nrobot.txt forbidden crawl\n");
            return false;
        }

        //clean
        Document.OutputSettings settings = document.outputSettings();
        settings.escapeMode(Entities.EscapeMode.extended);
        settings.charset("UTF-8");
        this.title = document.title();
        this.html_Source = document.html();
        this.text = document.body().text();
        log.append("\n" + title + "\n");
        return true;
    }

    private int keywordCount(String keyWords) {
        keyWords = keyWords.toLowerCase().replaceAll("[^a-zA-Z0-9]", " ");
        //System.out.println(keyWords);
        int count = 0;
        for (String word : keyWords.split(" ")) {
            PorterStemmer stemmer = new PorterStemmer();
            stemmer.setCurrent(word); //set string you need to stem
            stemmer.stem();  //stem the word
            String stemmedWord = stemmer.getCurrent();//get the stemmed word
            if (stemmedWord.toLowerCase().matches(
                    ".*\\b(chang|climat|sea|level|rise|global|warm|temperatur|estim|costal|ocean|ic)\\b.*")) {
                //System.out.println("match: " + stemmedWord);
                count++;
            }
        }
        return count;
    }

    public static String getDomainName(String url) {
        Matcher matcher;
        String DOMAIN_NAME_PATTERN
                = "([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,15}";
        Pattern patrn = Pattern.compile(DOMAIN_NAME_PATTERN);
        String domainName = "";
        matcher = patrn.matcher(url);
        if (matcher.find()) {
            domainName = matcher.group(0).toLowerCase().trim();
        }
        return domainName;
    }

    public void parseLinks() {
        log.append(domain);
        String type = HTTPheader.get("Content-Type");
        Elements linksOnPage = document.select("a");
        for (Element e : linksOnPage) {
            String nextUrl;
            nextUrl = e.attr("href");
            //get rid of empty and in web links
            if (nextUrl.length() == 0 || nextUrl.charAt(0) == '#') {
                continue;
            }
            //if single "/", add url to front
            if (nextUrl.charAt(0) == '/') {
                nextUrl = domain + nextUrl;
            }
            nextUrl = nextUrl.replaceAll("\\s+","");
            log.append(nextUrl + "\n");

            String titleAnchorTxt = "";
            try {
                String nextTitle = e.getElementById("title").toString();
                titleAnchorTxt = nextTitle;
            } catch (Exception noTitle) {
                //do nothing
            }
            String anchorTxt = e.text();
            titleAnchorTxt = titleAnchorTxt + " " + anchorTxt;
            int keywordcount = keywordCount(titleAnchorTxt);
            log.append("keywords:" + keywordcount + "\n");

            if (keywordcount < 2) {
                log.append("Ignored: not enough keywords\n");
                continue;
            }
            String nextDocno;
            try{
             nextDocno = Canonicalizer.apply(nextUrl);
            }catch (Exception cannonE) {
                log.append("Ignored: invalid url\n");
                continue;
            }
            //create new or add inlink
            if (!outElements.keySet().contains(nextUrl)) {
                Set<String> inUrl = new HashSet<>();
                inUrl.add(this.docno);
                QueElement qe = new QueElement(nextDocno, nextUrl, titleAnchorTxt, this.depth + 1, inUrl, keywordcount);
                //System.out.println(qe.toString());
                outElements.put(nextUrl, qe);
                outLinks.add(nextDocno);
            } else {
                QueElement qe = outElements.get(nextUrl);
                qe.getInLinks().add(this.docno);
                //System.out.println("added in link" + qe.toString());
                log.append("added in link" + qe.toString());
            }
        }
    }
    public void addInLinks(String inlink) {
        this.inLinks.add(inlink);
    }

    public String getId() {
        return docno;
    }

    public String getUrl() {
        return url;
    }

    public String getText() {
        return text;
    }

    public String getHtml_Source() {
        return html_Source;
    }

    public Set<String> getInLinks() {
        return inLinks;
    }

    public Set<String> getOutLinks() {
        return outLinks;
    }

    public Map<String, QueElement> getOutLinkElements() {
        return outElements;
    }

    public int getDepth() {
        return depth;
    }

    public String getTitle() {
        return title;
    }

    public Map<String, String> getHTTPheader() {
        return HTTPheader;
    }

    /**
     * Format string to store i
     * n Elastic Search.
     */
    @Override
    public String toString() {
        return String.format("wave %d title: %s \n %s\n", depth, title, url);
    }

    public String toJson() {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
        return gson.toJson(this);
    }

    private static void turnOffLogCommons() {
        //code for diable logging
        BasicConfigurator.configure();
        Logger.getLogger("ac.biu.nlp.nlp.engineml").setLevel(Level.OFF);
        Logger.getLogger("org.BIU.utils.logging.ExperimentLogger").setLevel(Level.OFF);
        Logger.getRootLogger().setLevel(Level.OFF);
    }


    private static void testKeywordsMatch() {
        String testkeywords = "Global Warming, ice temperature, estimates costal, ocean level, How do we know? Sea Rise Predictions- Climate Changes Change Changing: Vital Signs of the Planet: Evidence.";
        testkeywords = testkeywords.toLowerCase().replaceAll("[^a-zA-Z0-9]", " ");
        System.out.println(testkeywords);
        int count = 0;
        for (String word : testkeywords.split(" ")) {
            PorterStemmer stemmer = new PorterStemmer();
            stemmer.setCurrent(word); //set string you need to stem
            stemmer.stem();  //stem the word
            String stemmedWord = stemmer.getCurrent();//get the stemmed word
            System.out.println(stemmedWord);
            if (stemmedWord.toLowerCase().matches(".*\\b(chang|climat|sea|rise|ic)\\b.*")) {
                System.out.println("match: " + stemmedWord);
                count++;
            }
        }
        System.out.println(count);
    }


    public static void main(String[] s) throws Exception {
        turnOffLogCommons();
        //testKeywordsMatch();
        String testEmpty = "http://variable-variability.blogspot.com/2016/02/early-global-warming-transition-Stevenson-screens.html";
        String testUrl = "http://en.wikipedia.org/wiki/Climate_change";
        String simpleUrl = "https://www.simpleweb.org/";
        String googleSearch = "https://www.google.com/search?client=safari&rls=en&q=CLIMATE+CHANGE&ie=UTF-8&oe=UTF-8#q=CLIMATE+CHANGE&safe=off&rls=en&tbm=nws";
        String timeout = "http://ipcc.ch/robots.txt";

        //System.out.println(Canonicalizer.apply(googleSearch));
        //System.out.println(getDomainName(simpleUrl));
        //System.out.println(getDomainName(testUrl));

        try {
            Page p = new Page(testUrl, 0, new HashSet<>(), "testLogFile.txt",
                    new RobotReader(), new ArrayList<>());
            System.out.print(p.getText());
        } catch (Exception e) {
            e.printStackTrace();
        }
        //System.out.println(p.toString());
        //System.out.println(p.toJson());
        //FileUtils.write(new File("test.txt"),p.toJson());
        //FileUtils.write(new File("testOutlink.txt"), p.getOutLinks().toString());
        //System.out.println(p.getInLinks());
        //System.out.println(p.getOutLinks());
        //System.out.println(p.getId());
    }


}
