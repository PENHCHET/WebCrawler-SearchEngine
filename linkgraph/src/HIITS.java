package hw4;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
/**
 * Calculating authorities and hubs
 */
public class HIITS {
  private class PageObj {
    private String pageId;
    private double hubScore;
    private double authScore;
    private Set<String> inlinks;
    private Set<String> outlinks;

    public PageObj(String pageId) {
      this.pageId = pageId;
      this.inlinks = new HashSet<>();
      this.outlinks = new HashSet<>();
    }

    @Override
    public String toString() {
      return pageId + "\t" + hubScore + " " + authScore +"\n";
    }
  }

  private String graphPath = "LinkGraphFinal.txt";
  private String outAuthPath = "Auth500.txt";
  private String outHubPath = "Hub500.txt";
  private Map<String, Set<String>> outLinkMap;
  private Map<String, Set<String>> inLinkMap;
  private Map<String, PageObj> pageObjMap;
  private Map<String, Double> authorityScoreMap;
  private Map<String, Double> hubScoreMap;

  public HIITS() {
    hubScoreMap = new HashMap<>();
    authorityScoreMap = new HashMap<>();
    inLinkMap = new HashMap<>();
    outLinkMap = new HashMap<>();
    pageObjMap = new HashMap<>();
    getAllNodes();
    getAllInlinksOutlinks();
    createBase1000();
    while (authorityScoreMap.size() < 10000) {
      expandBase10000();
      System.out.println("build base: " + authorityScoreMap.size());
    }
    for(int i = 0; i < 10; i++) {
      updateHub();
      updateAuthority();
      normalizeAuth();
      normalizeHub();
    }
    produceListOfObject();
  }

  private void getAllNodes() {
    try {
      FileReader fileReader = new FileReader(new File(graphPath));
      Scanner scanner = new Scanner(fileReader);
      while (scanner.hasNext()) {
        String line = scanner.nextLine();
        String[] data = line.split(" ");
        String curPage = data[0];
        PageObj pageObj = new PageObj(curPage);
        pageObjMap.put(curPage, pageObj);
      }
      scanner.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void getAllInlinksOutlinks() {
    try {
      FileReader fileReader = new FileReader(new File(graphPath));
      Scanner scanner = new Scanner(fileReader);
      while (scanner.hasNext()) {
        String line = scanner.nextLine();
        String[] data = line.split(" ");
        String curPage = data[0];
        int numInLK = data.length - 1;
        if (numInLK > 0) {
          //in links
          Set<String> inlinks = new HashSet<>();
          for (int i = 1; i < data.length; i++) {
            inlinks.add(data[i]);
          }
          inLinkMap.put(curPage, inlinks);
          pageObjMap.get(curPage).inlinks = inLinkMap.get(curPage);

          //out links
          for (int i = 1; i < data.length; i++) {
            String page = data[i];
            if (!outLinkMap.containsKey(page)) {
              Set<String> outlinks = new HashSet<>();
              outlinks.add(curPage);
              outLinkMap.put(page, outlinks);
            } else {
              outLinkMap.get(page).add(curPage);
            }
            if (pageObjMap.get(page) != null) {
              pageObjMap.get(page).outlinks = outLinkMap.get(page);
            } else {
              PageObj newpage = new PageObj(page);
              newpage.outlinks = outLinkMap.get(page);
              pageObjMap.put(page, newpage);
            }
          }
        }
      }
      scanner.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  //1000 document by ES
  private void createBase1000() {
    String query = "climate change";
    RestClient restClient = RestClient.builder(
            new HttpHost("localhost", 9200, "http")).build();
    HttpEntity entity = new NStringEntity(
            "{\n"
                    + " \"size\" : 1000,\n"
                    + " \"_source\" : \"false\","
                    + " \"query\": {\n"
                    + "           \"match\": {\n"
                    + "            \"text\": \"" + query + "\"\n"
                    + "           }\n"
                    + "  }\n"
                    + "}", ContentType.APPLICATION_JSON);
    try {
      Response response = restClient.performRequest("GET",
              "/test/document/_search", Collections.<String, String>emptyMap(),
              entity);

      String str = EntityUtils.toString(response.getEntity());
      JsonParser parser = new JsonParser();
      JsonObject object = parser.parse(str).getAsJsonObject();
      JsonArray listOfDoc = object.getAsJsonObject("hits").getAsJsonArray("hits");

      for (JsonElement i : listOfDoc) {
        String key = i.getAsJsonObject().get("_id").getAsString();
        authorityScoreMap.put(key, 1.0);
        hubScoreMap.put(key, 1.0);
      }

      restClient.close();
      System.out.println("Build base 1000");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void expandBase10000() {
    List<String> newLinks = new ArrayList<>();
    //add all outlinks
    for (String web : hubScoreMap.keySet()) {
      newLinks.addAll(pageObjMap.get(web).outlinks);
    }

    //add 200 inLinks
    int d = 200;
    for (String web : hubScoreMap.keySet()) {
      Set<String> inlinks = pageObjMap.get(web).inlinks;
      if (inlinks.size() < d) {
        newLinks.addAll(inlinks);
      } else {
        List<String> list = new ArrayList<>(inlinks);
        Collections.shuffle(list);
        for (int i = 0; i < d; i++) {
          newLinks.add(list.get(i));
        }
      }
    }
    //add to set
    for (String str : newLinks) {
      authorityScoreMap.put(str, 1.0);
      hubScoreMap.put(str, 1.0);
    }
  }

  private void updateHub() {
    for (String hubweb : hubScoreMap.keySet()){
      Set<String> outlinks = pageObjMap.get(hubweb).outlinks;
      double sum = 0.0;
      for(String out : outlinks){
        if(authorityScoreMap.get(out) != null) {
          sum = sum + authorityScoreMap.get(out);
        }
      }
      hubScoreMap.put(hubweb, sum);
    }
  }
  private void updateAuthority() {
    for (String authWeb : authorityScoreMap.keySet()) {
      Set<String> inlinks = pageObjMap.get(authWeb).inlinks;
      double sum = 0.0;
      for(String in : inlinks){
        if(hubScoreMap.get(in) != null) {
          sum = sum + hubScoreMap.get(in);
        }
      }
      authorityScoreMap.put(authWeb, sum);
    }
  }

  private void normalizeHub(){
    double sum = 0.0;
    for(String hub : hubScoreMap.keySet()){
      double score = hubScoreMap.get(hub);
      sum = sum + score * score;
    }
    double factor = Math.sqrt(sum);
    for(String hub : hubScoreMap.keySet()){
      double newScore = hubScoreMap.get(hub)/factor;
      hubScoreMap.put(hub, newScore);
      //pageObjMap.get(hub).hubScore = newScore;
    }
    double sum2 = 0;
    for(String hub : hubScoreMap.keySet()){
      sum2 = hubScoreMap.get(hub) * hubScoreMap.get(hub) + sum2;
    }
    System.out.println("HUB Normalized total" + sum2);

  }
  private void normalizeAuth(){
    double sum = 0.0;
    for(String aut : authorityScoreMap.keySet()){
      double score = authorityScoreMap.get(aut);
      sum = sum + score * score;
    }
    double factor = Math.sqrt(sum);
    for(String aut : authorityScoreMap.keySet()){
      double newScore = authorityScoreMap.get(aut)/factor;
      authorityScoreMap.put(aut, newScore);
      //pageObjMap.get(aut).authScore = newScore;
    }
    double sum2 = 0;
    for(String aut : authorityScoreMap.keySet()){
      sum2 = authorityScoreMap.get(aut) * authorityScoreMap.get(aut) + sum2;
    }
    System.out.println("Auth Normalized total" + sum2);

  }

  private void produceListOfObject(){
    Map<String, PageObj> resultList = new HashMap<>();
    for(String str : hubScoreMap.keySet()){
      PageObj page = new PageObj(str);
      page.hubScore = hubScoreMap.get(str);
      page.authScore = 0.0;
      resultList.put(str, page);
    }

    for(String str : authorityScoreMap.keySet()){
      if(resultList.get(str) == null) {
        PageObj page = new PageObj(str);
        page.hubScore = 0.0;
        page.authScore = authorityScoreMap.get(str);
        resultList.put(str, page);
      } else {
        resultList.get(str).authScore = authorityScoreMap.get(str);
      }
    }

    List<PageObj> list = new LinkedList<PageObj>(resultList.values());
    Collections.sort(list, new Comparator<PageObj>() {
      @Override
      public int compare(PageObj o1, PageObj o2) {
        if(o1.hubScore > o2.hubScore){
          return -1;
        } else if (o1.hubScore < o2.hubScore){
          return 1;
        } else {
          return 0;
        }
      }
    });
    writeHub(list, outHubPath);

    List<PageObj> listAuth = new LinkedList<PageObj>(resultList.values());
    Collections.sort(listAuth, new Comparator<PageObj>() {
      @Override
      public int compare(PageObj o1, PageObj o2) {
        if(o1.authScore > o2.authScore){
          return -1;
        } else if(o1.authScore < o2.authScore) {
          return 1;
        } else {
          return 0;
        }
      }
    });
    writeAuthority(listAuth, outAuthPath);
  }

  private void writeHub(List<PageObj> list, String outputPath){
    StringBuilder sb = new StringBuilder();
    int count = 0;
    for(PageObj page : list) {
      if(count == 500) {
        break;
      }
      sb.append(page.pageId);
      sb.append(" ");
      sb.append(page.hubScore);
      sb.append("\n");
      count ++;
    }
    try {
      FileUtils.write (new File(outputPath), sb.toString());
      System.out.println("Wrote " + outputPath);
    }catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void writeAuthority(List<PageObj> list, String outputPath){
    StringBuilder sb = new StringBuilder();
    int count = 0;
    for(PageObj page : list) {
      if(count == 500) {
        break;
      }
      sb.append(page.pageId);
      sb.append(" ");
      sb.append(page.authScore);
      sb.append("\n");
      count ++;
    }

    try {
      FileUtils.write (new File(outputPath), sb.toString());
      System.out.println("Wrote " + outputPath);
    }catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void turnOffLogCommons() {
    //code for diable logging
    BasicConfigurator.configure();
    Logger.getLogger("ac.biu.nlp.nlp.engineml").setLevel(Level.OFF);
    Logger.getLogger("org.BIU.utils.logging.ExperimentLogger").setLevel(Level.OFF);
    Logger.getRootLogger().setLevel(Level.OFF);
  }
  public static void main(String[] str) {
    turnOffLogCommons();
    HIITS hiits = new HIITS();
  }


}

