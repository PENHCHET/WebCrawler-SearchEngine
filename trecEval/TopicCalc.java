package hw5;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Calculation for R-precision, Avg-precision, nDCG,
 * precision@k, recal @k, F1
 */
public class TopicCalc {
  private int topicNumber;
  private int[] rMatrix;
  private double[] precisionMatrix;
  private double[] recalMatrix;
  private double[] f1Matrix;
  private double avgPrec;
  private double rPrec;
  private double nDcg;
  private final int[] kvals =  {5,10,20,50,100};
  private int r;

  public TopicCalc(int[] rMatrix, int r, int topicNumber) {
    this.topicNumber = topicNumber;
    this.rMatrix = rMatrix;
    calcRecalMatrix(r);
    calcPrecisionMatrix();
    calcF1Matrix();
    calcAvgPrecision(r);
    calcR(r);
    calnDcg();
    this.r = r;
  }

  private void calcR(int r) {
    rPrec = recalMatrix[r];
  }

  /**
   * total number of relevant at k / number of files seen
   */
  private double[] calcPrecisionMatrix() {
    precisionMatrix = new double[rMatrix.length];
    double relSum = 0;
    for(int i = 0; i < rMatrix.length; i++) {
      relSum = relSum + rMatrix[i];
      double k = i + 1;
      precisionMatrix[i] = relSum/k;
    }
    return precisionMatrix;
  }
  /**
   * total number of relevant at k / R
   */
  private double[] calcRecalMatrix(int r) {
    recalMatrix = new double[rMatrix.length];
    double relSum = 0;
    for (int i = 0; i < rMatrix.length; i++) {
      relSum = relSum + rMatrix[i];
      recalMatrix[i] = relSum/r;
    }
    return recalMatrix;
  }
  private double[] calcF1Matrix() {
    f1Matrix = new double[rMatrix.length];
    for (int i = 0; i < rMatrix.length; i++) {
      double num = 2 * precisionMatrix[i] * recalMatrix[i];
      double denom = precisionMatrix[i] + recalMatrix[i];
      //System.out.println(recalMatrix[i]);
      //System.out.println(precisionMatrix[i]);
      f1Matrix[i] = num / denom;
      if (denom == 0) {
        f1Matrix[i] = 0;
      }
      //System.out.println(f1Matrix[i]);
    }
    return f1Matrix;
  }

  /**
   * 1/R * precision when relevance = 1
   */
  private void calcAvgPrecision(int r) {
    double sum = 0;
    for (int i = 0; i < rMatrix.length; i++) {
      if (rMatrix[i] > 0) {
        sum = sum + precisionMatrix[i];
      }
    }
    avgPrec =  sum / r;
  }
  private double calcDcg(int[] r) {
    double result =  r[0];
    for (int i = 1; i < r.length; i++) {
      if (r[i] == 0) {
        continue;
      }
      double lg = Math.log(i+1) / Math.log(2);
      result = result + (double) r[i] / lg;
    }
    return result;
  }

  private void calnDcg() {
    double part1 = calcDcg(rMatrix);
    List<Integer> m2 = new LinkedList<>();
    for (int i = 0; i < rMatrix.length; i ++) {
      m2.add(rMatrix[i]);
    }
    m2.sort(new Comparator<Integer>() {
      @Override
      public int compare(Integer o1, Integer o2) {
        if (o1 < o2) {
          return 1;
        } else if (o1.equals(o2)) {
          return 0;
        } else {
          return -1;
        }
      }
    });
    int[]r2 = new int[rMatrix.length];
    for (int i = 0; i < rMatrix.length; i++) {
      r2[i] = m2.get(i);
    }
    double part2 = calcDcg(r2);
    nDcg = part1 / part2;
  }

  public int[] getrMatrix() {
    return rMatrix;
  }

  public double[] getPrecisionMatrix() {
    return precisionMatrix;
  }

  public double[] getRecalMatrix() {
    return recalMatrix;
  }

  public double[] getF1Matrix() {
    return f1Matrix;
  }

  public double getAvgPrec() {
    return avgPrec;
  }

  public double getrPrec() {
    return rPrec;
  }

  public int getTopicNumber() {
    return topicNumber;
  }

  public double getnDcg() {
    return nDcg;
  }
}
