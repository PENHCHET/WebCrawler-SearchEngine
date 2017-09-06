package hw3;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The class craws a web-page into page object.
 */
public class Crawler {
    //hashes are by donoId
    //private static final String googleSearch = "https://www.google.com/search?client=safari&rls=en&q=CLIMATE+CHANGE&ie=UTF-8&oe=UTF-8#q=CLIMATE+CHANGE&safe=off&rls=en&tbm=nws";
    //private static final String googleSearchID = "comgooglesearchieUTF8oeUTF8qCLIMATECHANGE";
    private static final String crawlSpecs = "crawl_specs.txt";
    private static final String crawlLinks = "crawl_linkmap.txt";
    private Queue<QueElement> queue;
    private HashSet<String> visitedWebId;
    private HashMap<String, Page> crawledResultMap;
    private HashMap<String, Set<String>> inLinkMap;
    private HashMap<String, Set<String>> outLinkMap;
    private StringBuilder crawResultWriter;
    private List<Page> resultPages;
    private RobotReader robotReader;
    private HashMap<String, Long> timerDomainMap;

    StringBuilder resultSpec;
    private int crawlCount;
    private List<String> forbidenWeb;
    private long timeStart;
    private int waitingTimeTotal;
    private int maxInLinkNum;
    private int maxOutLinkNum;
    private String maxInLinkUrl;
    private String maxOutLinkUrl;
    private int delay;


    public Crawler(List<List<String>> seedUrlList) throws IOException {
        resultPages = new ArrayList<>();
        forbidenWeb = new ArrayList<>();
        resultSpec = new StringBuilder();
        timeStart = System.currentTimeMillis();
        waitingTimeTotal = 0;
        //priority based on keywords match.
        queue = new PriorityQueue<>(new Comparator<QueElement>() {
            @Override
            public int compare(QueElement o1, QueElement o2) {
                int score1 = o1.getWaveNum() * 100 - o1.getKeywordCount();
                int score2 = o2.getWaveNum() * 100 - o2.getKeywordCount();
                return score1 - score2;
            }
        });
        //queue = new LinkedList<>();
        crawledResultMap = new HashMap<>();
        visitedWebId = new HashSet<>();
        inLinkMap = new HashMap<>();
        outLinkMap = new HashMap<>();
        robotReader = new RobotReader();
        crawResultWriter = new StringBuilder();
        timerDomainMap = new HashMap<>();
        String logPath = "crawl_log/0.txt";
        //adding out links of wave 0 -> wave 1
        //size = 2, i = 0 4 seed, i =1 9 google search seed,
        for (int i = 0; i < seedUrlList.size(); i++) {
            for (String seedUrl : seedUrlList.get(i)) {
                Set<String> inlinks = new HashSet<>();
                try {
                    //sleep
                    wait1sec(seedUrl);
                    //crawl seed
                    Page seedPage = new Page(seedUrl, i, inlinks, logPath, robotReader, forbidenWeb);
                    System.out.println(String.format("wave:%d %s\n%s", i, seedPage.getId(), seedPage.getUrl()));
                    for(QueElement e: seedPage.getOutLinkElements().values()){
                        if(!visitedWebId.contains(e.getDocno())){
                            queue.add(e);
                            visitedWebId.add(e.getDocno());
                        }
                    }
                    crawledResultMap.put(seedPage.getId(), seedPage);
                    visitedWebId.add(seedPage.getId());
                    //create in link and out link map
                    inLinkMap.put(seedPage.getId(), seedPage.getInLinks());
                    outLinkMap.put(seedPage.getId(), seedPage.getOutLinks());
                } catch (Exception e) {
                    //page not valid, log info, move on to the next one.
                }
                System.out.println("number crawled: " +  crawledResultMap.size());
            }
        }
    }

    public void crawl(int THRESHOLD) {
        makeDir("crawl_log");
        String logPath = "crawl_log/0.txt";
        makeDir("crawl_result");
        String resultPath = "crawl_result/0.txt";
        crawlCount = crawledResultMap.size();
        System.out.println(crawlCount);
        int webPerFile = crawledResultMap.size();
        int fileCount = 0;
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
        while (crawlCount <= THRESHOLD) {
            //if (webPerFile == 200) {
                if (webPerFile == 4) {
                webPerFile = 0;
                resultPages.addAll(crawledResultMap.values());
                File file = new File(resultPath);
                try {

                    String json = gson.toJson(resultPages);
                    crawResultWriter.append(json);
                    FileUtils.writeStringToFile(file, crawResultWriter.toString(), "UTF-8");
                    System.out.println(crawledResultMap.size() + " file wrote");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                fileCount++;
                logPath = "crawl_log/" + fileCount + ".txt";
                resultPath = "crawl_result/" + fileCount + ".txt";
                resultPages = new ArrayList<>();
                crawResultWriter = new StringBuilder();
                crawledResultMap = new HashMap<>();
                if (crawlCount == THRESHOLD) {
                    break;
                }
            }

            QueElement element = queue.poll();
            if (element == null) {
                System.out.println("Finished Queue");
                break;
            }
            if (inLinkMap.keySet().contains(element.getDocno())){
                System.out.println("special duplicate");
                continue;
            }

            System.out.println(String.format("wave:%d qsize: %d %s\n%s", element.getWaveNum(), queue.size(), element.getTitle(), element.getWeburl()));
            //sleep
            wait1sec(element.getWeburl());

            //crawl new page.
            Page newPage;
            try {
                newPage = new Page(element.getWeburl(), element.getWaveNum(), element.getInLinks(), logPath, robotReader, forbidenWeb);
                inLinkMap.put(newPage.getId(), newPage.getInLinks());
                outLinkMap.put(newPage.getId(), newPage.getOutLinks());
                crawledResultMap.put(newPage.getId(), newPage);
                crawlCount++;
                webPerFile++;
                System.out.println("number crawled: " + crawlCount);
            } catch (Exception e) {
                //page not valid, log info, move on to the next one.
                continue;
            }

            //out link => if exist, update in link, else, put in queue, mark visited.
            Map<String, QueElement> outElements = newPage.getOutLinkElements();
            for (QueElement outLink : outElements.values()) {
                String docno = outLink.getDocno();
                //if out link has been crawled, add this url to the in link.
                if (visitedWebId.contains(docno)) {
                    //System.out.println("found duplicate");
                    if (inLinkMap.containsKey(docno)) {
                        inLinkMap.get(docno).add(newPage.getId());
                    }
                    if (crawledResultMap.containsKey(docno)) {
                        crawledResultMap.get(docno).addInLinks(newPage.getId());
                    }
                } else {
                    //System.out.println("new web" + outLink.getWeburl());
                    visitedWebId.add(outLink.getDocno());
                    queue.add(outLink);
                    //System.out.println("queue size" + queue.size());
                }
            }
        }
        double totalTimeCrawled = (double) (System.currentTimeMillis() - timeStart) / 1000 / 3600;
        System.out.println("total waiting:" + waitingTimeTotal + " milisec");
        double totalTimeWaited = (double) waitingTimeTotal / 1000 / 60;
        resultSpec.append("Author: Qiuyuan Sophie\n");
        resultSpec.append("Total time crawled:" + totalTimeCrawled + " hour\n");
        resultSpec.append("Total waiting time:" + totalTimeWaited + " min\n");
        writeLinkMap();
        writeSpecs();
    }

    public class LinkMap {
        private String id;
        private Set<String> inLinks;
        private Set<String> outLinks;

        public LinkMap(String id, Set<String> inLinks, Set<String> outLinks) {
            this.id = id;
            this.inLinks = inLinks;
            this.outLinks = outLinks;
        }
    }

    private void wait1sec(String url) {

        String domain = Page.getDomainName(url);
        if(robotReader.getDomainRobotMap().get(domain) == null){
            delay = 1000;
        } else {
            delay = (int) robotReader.getDomainRobotMap().get(domain).getCrawlDelay() * 1000;
        }
        if (timerDomainMap.containsKey(domain)) {
            long timeDifference = System.currentTimeMillis() - timerDomainMap.get(domain);
            //System.out.println("last same domain was " + timeDifference + "milisec ago");
            if (timeDifference < 1000) {
                try {
                    long toSleep = Math.max(1000, delay) - timeDifference;
                    waitingTimeTotal += toSleep;
                    System.out.println("sleep " + toSleep);
                    TimeUnit.MILLISECONDS.sleep(toSleep);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            timerDomainMap.put(domain, System.currentTimeMillis());
        } else {
            timerDomainMap.put(domain, System.currentTimeMillis());
        }
    }

    private void writeLinkMap() {
        maxOutLinkNum = 0;
        maxInLinkNum = 0;
        maxInLinkUrl = "";
        maxOutLinkUrl = "";
        List<LinkMap> linkMapList = new ArrayList<>();
        StringBuilder linkMap = new StringBuilder();
        for (String key : inLinkMap.keySet()) {
            int inum = inLinkMap.get(key).size();
            if (inum > maxInLinkNum) {
                maxInLinkNum = inum;
                maxInLinkUrl = key;
            }
            int onum = outLinkMap.get(key).size();
            if (onum > maxOutLinkNum) {
                maxOutLinkNum = onum;
                maxOutLinkUrl = key;
            }
            LinkMap newMap = new LinkMap(key, inLinkMap.get(key), outLinkMap.get(key));
            linkMapList.add(newMap);
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(linkMapList);
        linkMap.append(json);
        try {
            FileUtils.writeStringToFile(new File(crawlLinks), linkMap.toString());
            System.out.println("Wrote link map");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeSpecs() {
        resultSpec.append("number of web page crawled:" + crawlCount + "\n");
        resultSpec.append("number of domains:" + robotReader.getNumDomain() + "\n");
        resultSpec.append("page with most valid in links: " + maxInLinkNum + "\n");
        resultSpec.append(maxInLinkUrl + "\n");
        resultSpec.append("page with most valid out links:" + maxOutLinkNum + "\n");
        resultSpec.append(maxOutLinkUrl + "\n");
        resultSpec.append("number of forbidden: " + forbidenWeb.size() + "\n");
        for (String str : forbidenWeb) {
            resultSpec.append(str);
            resultSpec.append("\n");
        }
        try {
            FileUtils.writeStringToFile(new File(crawlSpecs), resultSpec.toString());
            System.out.println("Wrote " + crawlSpecs);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<List<String>> setUpDemo() {
        List<List<String>> result = new ArrayList<>();
        String testUrl1 = "http://en.wikipedia.org/wiki/Climate_change";
        //String testUrl2 = "https://www.google.com/search?client=safari&rls=en&q=CLIMATE+CHANGE&ie=UTF-8&oe=UTF-8#q=CLIMATE+CHANGE&safe=off&rls=en&tbm=nws";
        String testUrl3 = "http://en.wikipedia.org/wiki/Future_sea_level";
        String testUrl4 = "http://www.skepticalscience.com/sea-level-rise-predictions.htm";
        String testUrl5 = "http://www.epa.gov/climatechange/science/future.html";
        List<String> wave0 = Arrays.asList(testUrl1, testUrl3, testUrl4, testUrl5);

        result.add(wave0);
        return result;
    }

    private static List<List<String>> setUp() {
        List<List<String>> result = new ArrayList<>();
        String testUrl1 = "http://en.wikipedia.org/wiki/Climate_change";
        //robot forbidden
        String testUrl2 = "https://www.google.com/search?client=safari&rls=en&q=CLIMATE+CHANGE&ie=UTF-8&oe=UTF-8#q=CLIMATE+CHANGE&safe=off&rls=en&tbm=nws";
        String testUrl3 = "http://en.wikipedia.org/wiki/Future_sea_level";
        String testUrl4 = "http://www.skepticalscience.com/sea-level-rise-predictions.htm";
        String testUrl5 = "http://www.epa.gov/climatechange/science/future.html";
        List<String> wave0 = Arrays.asList(testUrl1, testUrl2, testUrl3, testUrl4, testUrl5);

        String url1 = "https://climate.nasa.gov/evidence/";
        String url2 = "http://www.ipcc.ch/";
        String url3 = "https://en.wikipedia.org/wiki/Climate_change"; //duplicate
        String url4 = "https://www.theguardian.com/environment/climate-change";
        String url5 = "https://www.takepart.com/flashcards/what-is-climate-change/";
        String url6 = "https://www.nytimes.com/section/climate";
        String url7 = "https://www.ecowatch.com/climate-change/";
        String url8 = "http://www.nationalgeographic.com/environment/climate-change/";
        String url9 = "http://www.independent.co.uk/news/world/world-food-supplies-climate-change-international-trade-global-warming-chatham-house-chokepoints-a7808221.html";
        String url10 = "http://www.un.org/sustainabledevelopment/climate-change-2/";
        List<String> wave1 = Arrays.asList(url1, url2, url3, url4, url5, url6, url7, url8, url9, url10);

        result.add(wave0);
        result.add(wave1);
        return result;
    }

    private static void turnOffLogCommons() {
        //code for diable logging
        BasicConfigurator.configure();
        Logger.getLogger("ac.biu.nlp.nlp.engineml").setLevel(Level.OFF);
        Logger.getLogger("org.BIU.utils.logging.ExperimentLogger").setLevel(Level.OFF);
        Logger.getRootLogger().setLevel(Level.OFF);
    }

    public static void makeDir(String folderName) {
        File resultFolder = new File(folderName);
        if (!resultFolder.exists()) {
            if (!resultFolder.mkdir()) {
                System.out.println("Failed to create directory" + resultFolder.toString());
            }
        }
    }

    public static void main(String[] arg) throws Exception {
        //int threshold = 20000;
        //int threshold = 12;
        turnOffLogCommons();
        //Crawler myCrawler = new Crawler(setUp());
        Crawler myCrawler = new Crawler(setUpDemo());
       // myCrawler.crawl(4);
    }
}

