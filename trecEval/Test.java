package hw5;

public class Test {
  int[] r;
  TopicCalc trec;
  public Test(int Rval) {
    r = new int[] {1,0,0,1,0};
    //r = new int[] {2,0,0,3,0};
    int R = Rval;
    trec = new TopicCalc(r, R , 101);
  }

  /**
   * Expecting: 1, 1/2, 1/3, 2/4, 2/5
   */
  private void testPrecision(){
    for (int i = 0; i < r.length; i++) {
      double[] prec= trec.getPrecisionMatrix();
      System.out.print(prec[i] + " ");
    }
    System.out.println();
  }

  /**
   * Expecting: 1/5, 1/5, 1/5, 2/5, 2/5
   */
  private void testRecal(){
    for (int i = 0; i < r.length; i++) {
      double[] rec= trec.getRecalMatrix();
      System.out.print(rec[i] + " ");
    }
    System.out.println();
  }
  /**
   * 2 * pre * rec / (pre + rec)
   * 0.33 0.28 0.25 0.44 0.40
   */
  private void testF1(){
    for (int i = 0; i < r.length; i++) {
      double[] f= trec.getF1Matrix();
      System.out.print(f[i] + " ");
    }
    System.out.println();
  }

  /**
   * Expecting: 1/5 * ( 1/1 + 2/4) = 0.3
   */
  private void testAvgPre(){
    System.out.println(trec.getAvgPrec());
  }

  /**
   * Expecting: 0.4
   */
  private void testR(){
    System.out.println(trec.getrPrec());
  }

  /**
   * 1 + 0 + 0 + 1/(log4) = 1.5
   * 1 + 1/log2 + 0 + 0 + 0 = 2
   * 1.5/2 = 3/4 = 0.75
   */
  private void testndcg(){
    System.out.println(trec.getnDcg());
  }
  public static void main(String[] str) {
    Test t = new Test(5);
    t.testPrecision();
    t.testRecal();
    t.testAvgPre();
    t.testF1();
    t.testR();
    t.testndcg();
  }
}
