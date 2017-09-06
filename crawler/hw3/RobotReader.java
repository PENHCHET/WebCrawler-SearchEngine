package hw3;

import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.SimpleRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;
import org.apache.http.entity.BufferedHttpEntity;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Reader for robots.txt, takes in domain and a test url.
 * Keep an internal hash map of seen robots.txt.
 */
public class RobotReader {
    private static final String MYCRAWLER = "Googlebot";
    private Map<String, BaseRobotRules> domainRobotMap;


    public RobotReader() {
        domainRobotMap = new HashMap<>();
    }

    //if time out, return true.
    public boolean run(String domain, String testUrl) {
        SimpleRobotRulesParser robotParser = new SimpleRobotRulesParser();
        BaseRobotRules robotRules;
        if (domainRobotMap.containsKey(domain)) {
            robotRules = domainRobotMap.get(domain);
        } else {
            try {
                String robotUrl = "http://" + domain + "/robots.txt";
                Connection connection = Jsoup.connect(robotUrl).timeout(1000);
                Connection.Response response = connection.execute();
                robotRules = robotParser.parseContent(domain, response.bodyAsBytes(), "text/plain", MYCRAWLER);
                domainRobotMap.put(domain, robotRules);
            } catch (Exception e) {
                //e.printStackTrace();
                robotRules = new SimpleRobotRules(SimpleRobotRules.RobotRulesMode.ALLOW_ALL);
                domainRobotMap.put(domain, robotRules);
            }
        }
        return robotRules.isAllowed(testUrl);
    }

    public int getNumDomain() {
        return domainRobotMap.keySet().size();
    }

    public Map<String, BaseRobotRules> getDomainRobotMap() {
        return domainRobotMap;
    }

    public static void main(String[] arg) throws Exception {

        String twiter = "www.twitter.com";
        String google = "www.google.com";
        String wiki = "en.wikipedia.org";

        String twittertest = "https://twitter.com/search/realtime";
        String wikitest = "https://en.wikipedia.org/wiki/Wikipedia:Benutzersperrung/";
        String WEBGOOGLE = "https://www.google.com/search?client=safari&rls=en&q=CLIMATE+CHANGE&ie=UTF-8&oe=UTF-8#q=CLIMATE+CHANGE&safe=off&rls=en&tbm=nws";
        String timeout = "http://ipcc.ch";
        String testUrl1 = "http://en.wikipedia.org/wiki/Climate_change";
        String seed = "http://en.wikipedia.org/wiki/Future_sea_level";

        RobotReader robotReader = new RobotReader();
        //System.out.println(robotReader.run(twiter, twittertest));
        //System.out.println(robotReader.run(wiki, wikitest));
        //System.out.println(robotReader.run(google, WEBGOOGLE));
        System.out.println(robotReader.run(wiki, testUrl1));
        System.out.println(robotReader.run(wiki, seed));

        //System.out.println(robotReader.run(wiki, WEBWIKI));
        //System.out.println(robotReader.run(wiki, WEBWIKI2));
        //System.out.println(robotReader.run(timeout, WEBGOOGLE));
    }
}
