package hw3;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpHost;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CreateLinkMap {
  private Gson gson ;
  private List<String> allNodes;
  private Map<String, LinkMap> linkMapMap;
  private final String inFilePath = "/Users/sophie/Desktop/allDocno.txt";
  private final String outFilePath = "finalLinkmap.txt";
  private final String authorFilePath = "finalAuthor.txt";
  private RestClient restClient;
  private Map<String, Integer> authorMap;

  public class LinkMap {
    private String docno;
    private Set<String> in_links;
    private Set<String> out_links;
    private String author;
    public LinkMap(String docno, Set<String> in_Links, Set<String> out_Links, String author) {
      this.docno = docno;
      this.in_links = in_Links;
      this.out_links = out_Links;
      this.author = author;
    }
    @Override
    public String toString(){
      return String.format("%s\n%s\ninlinks:\n%s\noutlinks:\n%s",docno,author,in_links,out_links);
    }
  }

  public CreateLinkMap(){
    gson = new GsonBuilder().setPrettyPrinting().create();
    allNodes = new ArrayList<>();
    linkMapMap = new HashMap<>();
    authorMap = new HashMap<>();
    restClient = RestClient.builder(
            new HttpHost("localhost", 9200, "http")).build();
  }


  public LinkMap getES(String docno){
    //loop through, count unique,record mapping
    LinkMap result = null;
    try {
      Response response = restClient.performRequest("GET",
              "/test/document/" + docno + "?_source_include=docno,author,out_links,in_links", Collections.emptyMap());
      String str = EntityUtils.toString(response.getEntity());
      JsonParser parser = new JsonParser();
      JsonObject object = parser.parse(str).getAsJsonObject();
      JsonObject data = object.getAsJsonObject("_source");
      //System.out.println(data.toString());
      result = gson.fromJson(data, LinkMap.class);
      //System.out.println(result.toString());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return result;
  }

  public void create(){
    //get all files, json
    JsonParser jsonParser = new JsonParser();
    try {
      JsonElement file = jsonParser.parse(new FileReader(new File(inFilePath)));
      JsonObject obj = (JsonObject) file;
      JsonArray docnoArray = obj.getAsJsonObject("hits").getAsJsonArray("hits");
      System.out.println(docnoArray.size());
      for (JsonElement i : docnoArray) {
        String docno = i.getAsJsonObject().get("_id").getAsString();
        LinkMap item = getES(docno);
        if(item!= null){
          linkMapMap.put(docno, item);
          String author = item.author;
          if(authorMap.containsKey(author)){
            authorMap.put(author,(authorMap.get(author) + 1));
          } else {
            authorMap.put(author, 1);
          }
          System.out.println(linkMapMap.size());
        }
        //break;
      }
    }catch (IOException e){
      e.printStackTrace();
    }
    write();
    closeConnection();
  }
  private void write(){
    try {
      FileUtils.write(new File(outFilePath), gson.toJson(linkMapMap));
      FileUtils.write(new File(authorFilePath), gson.toJson(authorMap));
    }catch (IOException e){
      e.printStackTrace();
    }
  }
  private void closeConnection() {
    try {
      this.restClient.close();
    } catch (IOException e) {
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
  public static void main(String[] str){
    turnOffLogCommons();
    CreateLinkMap linkMap = new CreateLinkMap();
    linkMap.create();
  }

}
