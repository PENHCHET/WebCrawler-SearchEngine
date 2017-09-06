package hw4;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;

/**
 * Calculating page rank using Iterative solution.
 */
public class PageRank {
  private class PageObj implements Comparable<PageObj>{
    private String pageId;
    private Set<String> inlinks;
    private Set<String> outlinks;
    private double score;
    public PageObj(String pageId) {
      this.pageId = pageId;
      this.score = 0;
      this.inlinks = new HashSet<>();
      this.outlinks = new HashSet<>();
    }
    @Override
    public String toString(){
      return pageId + "\t" + score + "\n" /*+ inlinks.toString() + "\n" + outlinks.toString() + "\n"*/;
    }
    @Override
    public int compareTo(PageObj other) {
      if(this.score < other.score) {
        return 1;
      } else {
        return -1;
      }
    }
  }
  private String graphPath;
  private String outPath;
  private final double LAMDA = 0.85;
  private double sizeN;
  private Map<String, Set<String>> outLinkMap;
  private Map<String, Set<String>> inLinkMap;
  private Map<String, PageObj> pageObjMap;
  private Map<String, Double> resultPR2;
  private Map<String, Double> resultPR;
  private Set<PageObj> sinks;
  private List<Double> perplexity;
  public PageRank(String graphPath, String outPath){
    this.outPath = outPath;
    this.graphPath = graphPath;
    inLinkMap = new HashMap<>();
    outLinkMap = new HashMap<>();
    pageObjMap = new HashMap<>();
    resultPR = new HashMap<>();
    resultPR2 = new HashMap<>();
    perplexity = new ArrayList<>();
    getAllNodes();
    setupMatrixIndex();
    getSinks();
    initScore();
  }

  private void getAllNodes(){
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
    }catch (IOException e) {
      e.printStackTrace();
    }
  }
  //graph -> in links, out links， Map of <PageId, PageObject>
  private void setupMatrixIndex(){
    try{
      FileReader fileReader = new FileReader(new File(graphPath));
      Scanner scanner = new Scanner(fileReader);
      while(scanner.hasNext()){
        String line = scanner.nextLine();
        String[] data = line.split(" ");
        String curPage = data[0];
        int numInLK = data.length - 1;
        if(numInLK > 0) {
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
            if(pageObjMap.get(page) != null) {
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
    } catch (IOException e){
      e.printStackTrace();
    }
  }

  private void getSinks(){
    sinks = new HashSet<>();
    for(PageObj p : pageObjMap.values()){
      if(p.outlinks.size() == 0) {
        sinks.add(p);
      }
    }
    System.out.println(sinks.size());
  }

  private void initScore(){
    this.sizeN = (double)pageObjMap.size();
    double score  =  1/sizeN;
    for(PageObj page : pageObjMap.values()){
      resultPR.put(page.pageId, score);
      resultPR2.put(page.pageId, score);
      page.score = score;
    }
  }

  //calculate the top 500 page by rank score
  public void calculate(){
    while (!converge()){
      //update PR2
      for(String index : resultPR.keySet()){
        resultPR2.put(index, resultPR.get(index));
      }
      double sinkPR = 0;
      for(PageObj page : sinks) {
          sinkPR = sinkPR + resultPR2.get(page.pageId);
      }
      for(PageObj page : pageObjMap.values()){
        double score;
        score = (1 - LAMDA) / sizeN;
        score = score + LAMDA * sinkPR / sizeN;
        for(String str : page.inlinks) {
          if(outLinkMap.get(str) != null) {
            score = score + LAMDA * resultPR2.get(str) / outLinkMap.get(str).size();
          }
        }
        page.score = score;
        resultPR.put(page.pageId, score);
      }
    }
    StringBuilder resultScore = prepareWrite();
    write2file(resultScore.toString());
  }

  //convergence Test
  private boolean converge(){
    double sum = 0;
    for(double pr: resultPR.values()){
      sum = pr * log2(pr) + sum;
    }
    double perp = Math.pow(2, (sum * -1));
    System.out.println(perp);
    perplexity.add(perp);
    int size = perplexity.size();
    if(size < 4) {
      return false;
    }
    double a = perplexity.get(size - 1);
    double b = perplexity.get(size - 2);
    double c = perplexity.get(size - 3);
    double d = perplexity.get(size - 4);
    return  ((int)a == (int)b) && ((int)c == (int)d) && ((int)a == (int)c);
  }
  private double log2(double x){
    return Math.log(x) / Math.log(2);
  }
  private StringBuilder prepareWrite() {
    StringBuilder resultScore = new StringBuilder();
    Queue<PageObj> resultList = new PriorityQueue<>();
    for(PageObj p : pageObjMap.values()){
      resultList.add(p);
    }
    int i = 0;
    while(!resultList.isEmpty() && i < 500){
      PageObj page = resultList.remove();
      resultScore.append(i + " ");
      resultScore.append(page);
      i++;
    }
    return resultScore;
  }
  private void write2file(String data){
    try {
      FileUtils.write(new File(outPath), data);
    }catch (IOException e){
      e.printStackTrace();
    }
  }


  private void helper(int[] ints){
    for(int i = 0; i < ints.length; i++) {
      System.out.println(ints[i]);
    }
    List lst = Arrays.asList(ints);
    System.out.println(lst.size());
    for(Object i : lst){
      System.out.println(i);
    }
    //Set<Integer> set = new HashSet<>(lst);
    //System.out.println(set.size());*/
  }

  public static void main(String[] str){
    //PageRank pageRank = new PageRank("testGraph.txt");
    PageRank pageRank = new PageRank("wt2g_inlinks.txt", "test500.txt");
    pageRank.calculate();
    PageRank pageRank2 = new PageRank("LinkGraphFinal.txt", "my500.txt");
    pageRank2.calculate();

  }
}
