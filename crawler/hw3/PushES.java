package hw3;

import com.google.gson.*;

import com.sun.javafx.binding.StringFormatter;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Evaluator;
import org.omg.Messaging.SYNC_WITH_TRANSPORT;

import javax.crypto.NullCipher;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * A class to push data into elastic search.
 */
public class PushES {
  //private static final String resultPath = "C:\\Users\\Sophie\\Desktop\\crawl_result\\";
  //private static final String linkMapPath = "C:\\Users\\Sophie\\Desktop\\crawl_linkmap.txt";

  //private static final String resultPath = "/Users/sophie/Desktop/crawl_result/";
  //private static final String linkMapPath = "/Users/sophie/Desktop/crawl_linkmap.txt";
  private static final String resultPath = "crawl_result/";
  private static final String linkMapPath = "crawl_linkmap.txt";
  private Map<String, LinkMap> linkMapMap;
  private RestClient restClient;
  private StringBuilder log;

  public PushES() {
    log = new StringBuilder();
    linkMapMap = new HashMap<>();
    readLinkMap();
    restClient = RestClient.builder(
            new HttpHost("localhost", 9200, "http")).build();
  }

  public boolean webIDExist(String ID) {
    //search command whether page exist
    try {
      //if exist update author, in-link, out-link
      Response indexResponse = restClient.performRequest(
              "GET",
              "/demo/document/" + ID + "?_source_include=author,out_links,in_links",
              Collections.<String, String>emptyMap());
      String str = EntityUtils.toString(indexResponse.getEntity());
      JsonParser parser = new JsonParser();
      JsonObject object = parser.parse(str).getAsJsonObject().getAsJsonObject("_source");
      JsonArray out_links = object.getAsJsonArray("out_links");
      JsonArray in_links = object.getAsJsonArray("in_links");
      String author = object.get("author").getAsString();

      for (JsonElement link : out_links) {
        String outlink = link.getAsString();
        if (linkMapMap.get(ID).outLinks.contains(outlink)) {
          continue;
        }
        linkMapMap.get(ID).outLinks.add(outlink);
      }

      for (JsonElement link : in_links) {
        String inlink = link.getAsString();
        if (linkMapMap.get(ID).inLinks.contains(inlink)) {
          continue;
        }
        linkMapMap.get(ID).inLinks.add(inlink);
      }

      String[] strlist = author.split(" ");
      boolean hasName = false;
      for (String name : strlist) {
        if (name.equals("Qiuyuan")) {
          //already there
          hasName = true;
        }
      }
      if (!hasName) {
        author = author + " Qiuyuan";
      } /*else {
                author = author + " dup";
            }*/
      update(author, linkMapMap.get(ID), ID);
      return true;
    } catch (IOException e) {
      //e.printStackTrace();
      return false;
    }
  }

  private void closeConnection() {
    try {
      this.restClient.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void update(String author, LinkMap linkMap, String ID) {
    try {
      JsonArray inlinks = new JsonArray();
      for (String link : linkMap.inLinks) {
        inlinks.add(link);
      }
      JsonArray outlinks = new JsonArray();
      for (String link : linkMap.outLinks) {
        outlinks.add(link);
      }
      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("author", author);
      jsonObject.add("in_links", inlinks);
      jsonObject.add("out_links", outlinks);

      JsonObject docWrapper = new JsonObject();
      docWrapper.add("doc", jsonObject);

      HttpEntity updateObjectEntity = new NStringEntity(docWrapper.toString(), ContentType.APPLICATION_JSON);
      Response indexResponse = restClient.performRequest(
              "POST",
              "/demo/document/" + ID + "/_update",
              Collections.emptyMap(),
              updateObjectEntity);

      //String str = EntityUtils.toString(indexResponse.getEntity());
      //System.out.println(str);

    } catch (Exception e) {
      System.out.println("update failed");
      e.printStackTrace();
    }
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

    @Override
    public String toString() {
      return String.format("%s\n%s\n%s\n", id, inLinks.toString(), outLinks.toString());
    }
  }

  private void readLinkMap() {
    Gson gson = new Gson();
    String filePath = linkMapPath;
    //String filePath = testlinkPath;
    JsonParser jsonParser = new JsonParser();
    try {
      Object obj = jsonParser.parse(new FileReader(filePath));
      JsonArray jsonArray = (JsonArray) obj;
      for (JsonElement jsonElement : jsonArray) {
        JsonObject object = (JsonObject) jsonElement;
        LinkMap linkmap = gson.fromJson(object, LinkMap.class);
        linkMapMap.put(linkmap.id, linkmap);
        //System.out.println(linkmap.id);
      }
      //System.out.println(linkMapMap.toString());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void upload() {
    int newfileCount = 0;
    int updateCount = 0;
    long startTime = System.currentTimeMillis();
    JsonParser jsonParser = new JsonParser();
    try {
      //for (int i = 70; i < 80; i++) {
      for (int i = 0; i < 1; i++) {
        String filePath = resultPath + i + ".txt";
        //String filePath = "test.txt";
        //String filePath = testResultPath + i + ".txt";
        Object obj = jsonParser.parse(new FileReader(filePath));
        JsonArray jsonArray = (JsonArray) obj;
        for (JsonElement jsonElement : jsonArray) {
          JsonObject object = (JsonObject) jsonElement;
          if (webIDExist(object.get("docno").getAsString())) {
            log.append("updated a page\n");
            log.append(object.get("title") + "\n");
            log.append(object.get("url") + "\n");
            updateCount++;
          } else {
            log.append("write new page\n");
            log.append(object.get("title") + "\n");
            log.append(object.get("url") + "\n");
            push2ES(restClient, object);
            newfileCount++;
            //closeConnection();
            //return;
          }
        }
        log.append("progress: " + i + "/100 " + newfileCount + " new files usedTime:" + (System.currentTimeMillis() - startTime) / 1000 / 60 + " minutes \n");
        log.append("progress: " + i + "/100 " + updateCount + " updated files usedTime:" + (System.currentTimeMillis() - startTime) / 1000 / 60 + " minutes \n");
        System.out.println("progress: " + i + "/100 " + newfileCount + " new files usedTime:" + (System.currentTimeMillis() - startTime) / 1000 / 60 + " minutes");
        System.out.println("progress: " + i + "/100 " + updateCount + " updated files usedTime:" + (System.currentTimeMillis() - startTime) / 1000 / 60 + " minutes");
        FileUtils.write(new File("pushESlog.txt"), log.toString(), true);
      }
      System.out.println("done uploading");
    } catch (IOException e) {
      e.printStackTrace();
    }
    closeConnection();
  }


  public void push2ES(RestClient restClient, JsonObject page) {
    String docno = page.get("docno").getAsString();
    String title = "";
    String text = "";
    String html_Source = "";
    try {
      title = page.get("title").getAsString();
      text = page.get("text").getAsString();
      html_Source = page.get("html_Source").getAsString();
    } catch (NullPointerException e) {
      //field may not exist.
    }
    String url = page.get("url").getAsString();
    String author = "Qiuyuan";
    int depth = page.get("depth").getAsInt();
    JsonArray inlinks = new JsonArray();
    JsonArray outlinks = new JsonArray();
    if (linkMapMap.get(docno) == null) {
      System.out.println(docno);
      System.out.println(url);
    } else {
      for (String str : linkMapMap.get(docno).inLinks) {
        inlinks.add(str);
      }
      for (String str : linkMapMap.get(docno).outLinks) {
        outlinks.add(str);
      }
    }
    JsonObject header = page.getAsJsonObject("HTTPheader");
    JsonObject HTTPheader = new JsonObject();
    try {
      HTTPheader.addProperty("Server", header.get("Server").toString());
      HTTPheader.addProperty("Last-Modified", header.get("Last-Modified").toString());
      HTTPheader.addProperty("Date", header.get("Date").toString());
      HTTPheader.addProperty("Content-Type", header.get("Content-Type").toString());
      HTTPheader.addProperty("Content-Length", header.get("Content-Length").toString());
    } catch (NullPointerException emptyfield) {
      //some fields maybe missing.
    }
    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    JsonObject page2upload = new JsonObject();
    if (docno.length() > 512) {
      docno = docno.substring(0, 511);
    }
    page2upload.addProperty("docno", docno);
    page2upload.addProperty("title", gson.toJson(title));
    page2upload.addProperty("text", gson.toJson(text));
    page2upload.addProperty("html_Source", html_Source);
    page2upload.addProperty("url", url);
    page2upload.addProperty("author", author);
    page2upload.addProperty("depth", depth);
    page2upload.add("in_links", inlinks);
    page2upload.add("out_links", outlinks);
    page2upload.add("HTTPheader", HTTPheader);
    //System.out.println(page2upload.get(html_Source));
    //System.out.println(page2upload.toString());
    try {
      HttpEntity entity = new NStringEntity(page2upload.toString(), ContentType.APPLICATION_JSON);
      Response pushRespons = restClient.performRequest("PUT",
              "/demo/document/" + docno, Collections.emptyMap(),
              entity);
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println(page2upload.toString());
    }

  }

  private static void turnOffLogCommons() {
    //code for diable logging
    BasicConfigurator.configure();
    Logger.getLogger("ac.biu.nlp.nlp.engineml").setLevel(Level.OFF);
    Logger.getLogger("org.BIU.utils.logging.ExperimentLogger").setLevel(Level.OFF);
    Logger.getRootLogger().setLevel(Level.OFF);
  }

  public static void main(String[] str) throws Exception {
    turnOffLogCommons();
       /*Response response = restClient.performRequest("GET", "/",
                Collections.singletonMap("pretty", "true"));
        System.out.println(EntityUtils.toString(response.getEntity()));
        */
    PushES pushES = new PushES();
    //pushES.webIDExist("orgwikipediaenwikiFuturesealevel");
    pushES.upload();
    //putCommand(restClient);
    //File f = new File("C:\\Users\\Sophie\\Desktop\\crawl_specs.txt");

       /* File f = new File(resultPath + "0.txt");
        FileReader fr = new FileReader(f);
        Scanner sc = new Scanner(fr);
        System.out.println(sc.nextLine());*/

      /* byte[] limit = new byte[512];
       String test = "12abc3456789";
       int size = test.getBytes().length;
       System.out.println( size);*/
  }
}
