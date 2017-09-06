package hw5;

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
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * To get the top 200 relevant files from elastic search.
 * Write to file with docno, url.
 */
public class GetES {
  private final String outPath = "3000ES.txt";
  private Map<Integer, Map<String, Double>> result;
  private StringBuilder sb;
  public GetES() {
    sb = new StringBuilder();
    result = new HashMap<>();
    result.put(150501, getData("difference between weather and climate",150501));
    result.put(150502, getData("sea rise predictions",150502));
    result.put(150503, getData("human impact on climate",150503));
  }
  private Map<String, Double> getData(String query, Integer queryId){
    Map<String, Double> docnoScore = new HashMap<>();
    RestClient restClient = RestClient.builder(
            new HttpHost("localhost", 9200, "http")).build();
    HttpEntity entity = new NStringEntity(
            "{\n"
                    + " \"size\" : 1000,\n"
                    + " \"_source\" : \"url\","
                    + " \"query\": {\n"
                    + "           \"query_string\": {\n"
                    + "            \"query\": \"" + query + "\"\n"
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
      int rank = 1;
      for (JsonElement i : listOfDoc) {
        String key = i.getAsJsonObject().get("_id").getAsString();
        Double score = i.getAsJsonObject().get("_score").getAsDouble();
        sb.append(queryId).append(" Q0 ").append(key).append(" " + rank + " ").append(score).append(" Exp\n");
        docnoScore.put(key,score);
        rank++;
      }
      restClient.close();
      System.out.println("Get " + docnoScore.size() + "documents.");
    } catch (IOException e) {
      e.printStackTrace();
    }
    return docnoScore;
  }
  private void writeFile(String fileData){
    try {
      FileUtils.write(new File(outPath), fileData);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  private void write(){
    writeFile(sb.toString());
  }
  public static void main(String[] str) {
    GetES get = new GetES();
    get.write();
  }
}
