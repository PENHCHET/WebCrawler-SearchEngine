package hw5;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FileTranfer {

  private static Map<String, Integer> transfer(String inFilePath) {
    Map<String, Integer> map = new HashMap<>();
    try {
      JsonParser jsonParser = new JsonParser();
      JsonElement file = jsonParser.parse(new FileReader(new File(inFilePath)));
      JsonObject obj = (JsonObject) file;
      JsonArray docnoArray = obj.getAsJsonObject("hits").getAsJsonArray("hits");
      System.out.println(docnoArray.size());
      for (JsonElement i : docnoArray) {
        String docno = i.getAsJsonObject().get("_id").getAsString();
        int score = i.getAsJsonObject().get("_source").getAsJsonObject().get("rate").getAsInt();
        map.put(docno, score);
      }
    }catch (IOException e){
      e.printStackTrace();
    }
    return map;
  }
  private static void write (Map<Integer, Map<String, Integer>> map) {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    String json  = gson.toJson(map);
    try {
      FileUtils.write(new File("merged_qiuyuan.txt"), json);
    } catch (IOException e){
      e.printStackTrace();
    }
  }


  public static void main(String[] str){
    /*Map<Integer, Map<String, Integer>> map = new HashMap<>();
    Map<String, Integer> map1 = transfer("150501.txt");
    Map<String, Integer> map2 = transfer("150502.txt");
    Map<String, Integer> map3 = transfer("150503.txt");
    map.put(150501, map1);
    map.put(150502, map2);
    map.put(150503, map3);
    write(map);*/
  }
}
