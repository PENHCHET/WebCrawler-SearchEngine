package hw5;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Calculation for R-precision, Avg-precision, nDCG,
 * precision@k, recal @k, F1@k, k = 5, 10, 20, 50, 100
 */
public class TrecEval {
  private String qrelPath;
  private String rankPath;
  // topic -> total number of relevant file;
  private Map<Integer, Integer> numRelTabel;
  // topic -> doc number -> relevance
  private Map<Integer, Map<String, Integer>> relevanceMap;
  // topic -> doc number -> rank
  private Map<Integer, Map<String, Double>> rankMap;
  // arrays
  private final int[] kvals = {5, 10, 20, 50, 100};
  // topic list
  List<TopicCalc> topics;
  private StringBuilder result;
  private boolean q;
  private int totalRevl;
  private int totalRetrived;
  private int totalRelRetr;

  public TrecEval(String qrel, String rank, boolean q) {
    qrelPath = qrel;
    rankPath = rank;
    numRelTabel = new HashMap<>();
    relevanceMap = new HashMap<>();
    rankMap = new HashMap<>();
    topics = new ArrayList<>();
    result = new StringBuilder();
    this.q = q;
    totalRelRetr = 0;
    totalRetrived = 0;
    totalRevl = 0;
  }

  //fill up numRelTable: [topic] -> [totalNumRelFiles]
  //fill up docRelTable: [topic] -> [docId] -> [rel]
  private void readQrel() {
    try {
      Scanner scanner = new Scanner(new FileReader(qrelPath));
      while (scanner.hasNext()) {
        String line = scanner.nextLine();
        String val[] = line.split(" ");
        Integer topic = Integer.valueOf(val[0]);
        String docId = val[2];
        Integer rel = Integer.valueOf(val[3]);
        Map<String, Integer> docRel = new HashMap<>();
        if (relevanceMap.containsKey(topic)) {
          relevanceMap.get(topic).put(docId, rel);
        } else {
          docRel.put(docId, rel);
          relevanceMap.put(topic, docRel);
        }
        if (numRelTabel.containsKey(topic)) {
          //relevant can be 0 or 1
          if (rel == 0) {
            numRelTabel.put(topic, numRelTabel.get(topic) + rel);
          } else {
            numRelTabel.put(topic, numRelTabel.get(topic) + 1);
          }
        } else {
          numRelTabel.put(topic, rel);
        }
      }
      scanner.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void testQrelRead() {
    //test for reading relMap
    try {
      boolean correct = true;
      Scanner scanner = new Scanner(new FileReader(qrelPath));
      while (scanner.hasNext()) {
        String line = scanner.nextLine();
        String val[] = line.split(" ");
        Integer topic = Integer.valueOf(val[0]);
        String docId = val[2];
        int rel = Integer.valueOf(val[3]);
        if (relevanceMap.containsKey(topic)) {
          if (relevanceMap.get(topic).get(docId) != rel) {
            System.out.println("wrong relevance number: " + topic + " " + docId + " " + rel);
            correct = false;
          }
        } else {
          System.out.println("topic not found: " + topic);
          correct = false;
        }
      }
      scanner.close();
      if (correct) {
        System.out.println("load QREL table done");
      }
      System.out.println("topic \t number of relevance total");
      for (Integer topic : numRelTabel.keySet()) {
        System.out.println(topic + " " + numRelTabel.get(topic));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  private void readRank() {
    try {
      StringBuilder sb = new StringBuilder();
      Scanner sc = new Scanner(new FileReader(rankPath));
      while (sc.hasNext()) {
        String line = sc.nextLine();
        String[] vals = line.split(" ");
        Integer topic = Integer.valueOf(vals[0]);
        String docId = vals[2];
        double rank = Double.valueOf(vals[4]);
        if (rankMap.containsKey(topic)) {
          rankMap.get(topic).put(docId, rank);
          sb.append(topic + " " + docId + " " + rank + "\n");
        } else {
          Map<String, Double> idRank = new HashMap<>();
          idRank.put(docId, rank);
          rankMap.put(topic, idRank);
          sb.append(topic + " " + docId + " " + rank + "\n");
        }
      }
      //FileUtils.write(new File("test rank"), sb.toString());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void testReadRank() {
    try {
      boolean correct = true;
      Scanner sc = new Scanner(new FileReader(rankPath));
      while (sc.hasNext()) {
        String line = sc.nextLine();
        String[] vals = line.split(" ");
        Integer topic = Integer.valueOf(vals[0]);
        String docId = vals[2];
        int rank = Integer.valueOf(vals[3]);
        if (rankMap.containsKey(topic)) {
          if (rankMap.get(topic).get(docId) != rank) {
            correct = false;
            System.out.println("not found " + topic + " " + docId + " " + rank + "\n");
          }
        } else {
          correct = false;
          System.out.println("no topic found " + topic);
        }
      }
      if (correct) {
        System.out.println("readRankFile done");
        System.out.println("topic \t size of list");
        for (Integer topic : rankMap.keySet()) {
          System.out.println(topic + " " + rankMap.get(topic).size());
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void formTopicMatrix() {
    double sumR = 0.0;
    double sumAvg = 0.0;

    //sort topic by number, sort rank by rank val
    List<Integer> topics = new LinkedList<>(rankMap.keySet());
    topics.sort(new Comparator<Integer>() {
      @Override
      public int compare(Integer o1, Integer o2) {
        return o1 - o2;
      }
    });
    Map<Integer, double[]> precSum  = new HashMap<>();
    Map<Integer, double[]> recalSum =new HashMap<>();
    Map<Integer, double[]> f1Sum = new HashMap<>();
    double ndcgsum = 0;
    for (int topicNum : topics) {
      int R = numRelTabel.get(topicNum);
      List<String> docNums = new LinkedList<>(rankMap.get(topicNum).keySet());
      //for (int q = 0; q < 10; q++) {
      //  System.out.println(docNums.get(q) + " " + rankMap.get(topicNum).get(docNums.get(q)));
      //}
      //sort by score
      Collections.sort(docNums, new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
          double rank1 = rankMap.get(topicNum).get(o1);
          double rank2 = rankMap.get(topicNum).get(o2);
          if (rank1 < rank2) {
            return 1;
          }
          if (rank1 > rank2) {
            return -1;
          } else {
            return 0;
          }
        }
      });
      /*for (int q = 0; q < 10; q++) {
        System.out.println(docNums.get(q));
      }*/
      //r matrix
      int[] r = new int[docNums.size()];
      int i = 0;
      int relRetCount = 0;
      for (String doc : docNums) {
        int relevance = 0;
        if (relevanceMap.get(topicNum).containsKey(doc)) {
          relevance = relevanceMap.get(topicNum).get(doc) == 0 ? 0 : 1;
          relRetCount += relevance;
        }
        r[i] = relevance;
        i++;
      }
      TopicCalc topic = new TopicCalc(r, R, topicNum);
      double[] prec = topic.getPrecisionMatrix();
      double[] recal = topic.getRecalMatrix();
      StringBuilder pr1 = new StringBuilder();
      pr1.append("index,precision,recal\n");
      double nonDecPrec = 0;
      for (int index = 0; index < prec.length; index++) {
        pr1.append(index).append(",").append(prec[index]).append(",").append(recal[index]).append("\n");
      }
      String graphTitle = topic.getTopicNumber()+"_graph.csv";
      try{
        FileUtils.write(new File (graphTitle), pr1.toString());
      }catch (IOException e) {
        e.printStackTrace();
      }
      double[] f1 = topic.getF1Matrix();
      double ndcg = topic.getnDcg();
      ndcgsum = ndcgsum + ndcg;
      precSum.put(topicNum, prec);
      recalSum.put(topicNum, recal);
      f1Sum.put(topicNum, f1);
      sumAvg = sumAvg + topic.getAvgPrec();
      sumR = sumR + topic.getrPrec();
      totalRevl = totalRevl + R;
      totalRetrived = totalRetrived + docNums.size();
      totalRelRetr = totalRelRetr + relRetCount;

      if (q) {
        result.append("\n");
        result.append("QueryId: " + topicNum).append("\n");
        result.append("Retrieved: " + docNums.size()).append("\n");
        result.append("Relevant: " + numRelTabel.get(topicNum)).append("\n");
        result.append("Rel_ret: " + relRetCount).append("\n");

        result.append("precision: \n");
        for (int j : kvals) {
          result.append("at " + j + " precision " + prec[j]).append("\n");
        }
        result.append("recall: \n");
        for (int j : kvals) {
          result.append("at " + j + " recall " + recal[j]).append("\n");
        }
        result.append("F1: \n");
        for (int j : kvals) {
          result.append("at " + j + " F1 " + f1[j]).append("\n");
        }
        result.append("AvgPrecision: " + topic.getAvgPrec() + " R-Precision: " + topic.getrPrec()).append("\n");
        result.append("NDCG: " + ndcg + "\n");
      }//break;
    }
    result.append("\n");
    result.append("Total number of queries: " + rankMap.size()).append("\n");
    result.append("Total Retrieved: " + totalRetrived).append("\n");
    result.append("Total Relevant: " + totalRevl).append("\n");
    result.append("Total Rel_ret: " + totalRelRetr).append("\n");
    result.append("precision: \n");
    for (int j : kvals) {
      double precSumPerTopic = 0.0;
      for (Integer i : precSum.keySet()) {
        precSumPerTopic = precSumPerTopic + precSum.get(i)[j - 1];
      }
      result.append("at " + j + " precisionAvg " + precSumPerTopic / topics.size()).append("\n");
    }
    result.append("recall: \n");
    for (int j : kvals) {
      double recalAvg = 0.0;
      for (Integer i : recalSum.keySet()) {
        recalAvg = recalAvg + recalSum.get(i)[j - 1];
      }
      result.append("at " + j + " recallAvg " + recalAvg/ rankMap.size()).append("\n");
    }
    result.append("F1: \n");
    for (int j : kvals) {
      double fAvg = 0.0;
      for (Integer i : f1Sum.keySet()) {
        //System.out.println(f1Sum.get(i)[j - 1]);
        fAvg = fAvg + f1Sum.get(i)[j];
      }
      result.append("at " + j + " F1Avg " + fAvg / topics.size()).append("\n");
    }
    result.append("Avg-R-Precision: " + sumR / topics.size() + "\nAvg-Precision: " + sumAvg / rankMap.size());
    result.append("\nAvg-NDCG: " + ndcgsum/ topics.size());
    System.out.println(result.toString());
  }

  public static void main(String[] str) {
    String qrelPath;
    String resultPath;
    boolean q;
    if (str.length == 2) {
      qrelPath = str[0];
      resultPath = str[1];
      q = false;
      TrecEval jarTest = new TrecEval(qrelPath,resultPath, q);
      return;
    }
    if (str.length == 3){
      q = true;
      qrelPath = str[1];
      resultPath = str[2];
      TrecEval jarTest = new TrecEval(qrelPath,resultPath, q);
    } else {
      //TrecEval te = new TrecEval("team_qrels.txt", "3000ES.txt", true);
      TrecEval te = new TrecEval("myqrel.txt", "3000ES.txt", true);
      //TrecEval te = new TrecEval("qrels.adhoc.51-100.AP89.txt", "Okapi.txt", true);
      te.readQrel();
      //te.testQrelRead();
      te.readRank();
      //te.testReadRank();
      te.formTopicMatrix();
    }
  }
}
