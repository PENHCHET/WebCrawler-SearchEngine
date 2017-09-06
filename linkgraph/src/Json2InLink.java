package hw4;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Json link graph to in link format.
 */
public class Json2InLink {
  private Set<String> nodes;
  StringBuilder result;


  public Json2InLink(){
    result = new StringBuilder();
    nodes = new HashSet<>();
    getAllNodes("allDocno.txt");
  }
  private void getAllNodes(String inFilePath){
    try {
      JsonParser jsonParser = new JsonParser();
      JsonElement file = jsonParser.parse(new FileReader(new File(inFilePath)));
      JsonObject obj = (JsonObject) file;
      JsonArray docnoArray = obj.getAsJsonObject("hits").getAsJsonArray("hits");
      System.out.println(docnoArray.size());
      for (JsonElement i : docnoArray) {
        String docno = i.getAsJsonObject().get("_id").getAsString();
        nodes.add(docno);
      }
    }catch (IOException e){
      e.printStackTrace();
    }
  }

  private void readFile(String path){
    try {
      FileReader readable = new FileReader(new File(path));
      JsonParser parser = new JsonParser();
      JsonElement doc = parser.parse(readable);
      JsonObject listOfPage = (JsonObject) doc;
      for(String page: nodes) {
        result.append(page);
        result.append(" ");
        JsonObject pageObj = listOfPage.getAsJsonObject(page);
        JsonArray inlinks = pageObj.getAsJsonArray("in_links");
        for(JsonElement link : inlinks){
          String linkStr = link.toString();
          result.append(linkStr.replaceAll("\"",""));
          result.append(" ");
        }
        result.append("\n");
        //System.out.println(result.toString());
      }
    }catch (IOException e){
      e.printStackTrace();
    }
  }
  private void writeFile(String path) {
    try {
      FileUtils.write(new File(path), result.toString());
    }catch (IOException e){
      e.printStackTrace();
    }
  }

  public static void main(String[] s){
    Json2InLink json2InLink = new Json2InLink();
    json2InLink.readFile("LinkGraph.txt");
    json2InLink.writeFile("LinkGraphFinal.txt");
  }

}
