package hw5;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.lang.reflect.Type;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Merge 3 qrel files and run trec eval.
 */
public class MergeCalc {
  private final String path1 = "merged_qiuyuan.txt";
  private final String path2 = "merged_yao.txt";
  private final String path3 = "merged_yixing.txt";

  public  MergeCalc() {
  }
  private Map<Integer, Map<String, Integer>> readDoc(String path){
    Map<Integer, Map<String, Integer>> result = new HashMap<>();
    try {
      JsonReader jsonReader = new JsonReader(new FileReader(path));
      Type type = new TypeToken<HashMap<Integer, HashMap<String,Integer>>>(){}.getType();
      return new Gson().fromJson(jsonReader, type);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return result;
  }
  public void mergeAll() {
    Map<Integer, Map<String,Integer>> f1 = readDoc(path1);
    Map<Integer, Map<String, Integer>> f2 = readDoc(path2);
    Map<Integer, Map<String, Integer>> f3 = readDoc(path3);
    StringBuilder sqrel = new StringBuilder();
    StringBuilder sb = new StringBuilder();
    sb.append("queryId").append(",").append("docId").append(",").append("qiuyuan")
            .append(",").append("yao").append(",").append("yixing").append("\n");
    for (Map.Entry<Integer, Map<String, Integer>> e : f1.entrySet()) {
      int queryID = e.getKey();
      Map<String, Integer> t1 = f2.get(queryID);
      Map<String, Integer> t2 = f3.get(queryID);
      for (Map.Entry<String, Integer> e1 : e.getValue().entrySet()) {
        String doc = e1.getKey();
        int score1 = e1.getValue();
        int score2 = t1.get(doc) != null ? t1.get(doc) : 0;
        int score3 = t2.get(doc) != null ? t2.get(doc) : 0;
        int score = 0;
        if (score1 != score2 && score2 != score3) {
          score = 1;
        }
        if (score1 == score2 && score2 == score3) {
          score = score1;
        }
        if (score1 == score2 || score1 == score3) {
          score = score1;
        }
        if (score3 == score2 || score3 == score1) {
          score = score3;
        }
        if (score2 == score1 || score2 == score3) {
          score = score2;
        }
        //int score = Math.max(score1, score2);
        score = Math.max(score3, score);
        sqrel.append(queryID).append(" 0 ").append(doc).append(" ").append(score).append("\n");
        sb.append(queryID).append(",").append(doc).append(",").append(e1.getValue())
                .append(",").append(t1.get(doc)).append(",").append(t2.get(doc)).append("\n");
      }
    }
    try{
      FileUtils.write(new File("myqrel.csv"), sb.toString());
      FileUtils.write(new File("myqrel.txt"), sqrel.toString());
    } catch (IOException e) {
      e.toString();
    }
  }
  public static void main(String[] str) {
    MergeCalc mg = new MergeCalc();
    mg.mergeAll();
  }
}
