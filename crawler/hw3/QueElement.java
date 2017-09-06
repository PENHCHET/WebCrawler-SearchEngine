package hw3;

import com.google.gson.Gson;
import sun.awt.image.ImageWatched;

import java.util.HashSet;
import java.util.Set;

/**
 * A class representing frontier elements : weburl, title, wavenumber.
 */
public class QueElement {
    private String weburl;
    private String title;
    private int waveNum;
    private Set<String> inLinks;
    private int keywordCount;
    private String docno;

    public QueElement(String webID, String weburl, String title, int waveNum, Set<String> inLinks, int keywordCount) {
        this.weburl = weburl;
        this.title = title;
        this.waveNum = waveNum;
        this.inLinks = inLinks;
        this.docno = webID;
        this.keywordCount = keywordCount;
    }

    @Override
    public String toString(){
        return String.format("wave:%d %s\n%s\n", waveNum,title,weburl);
    }
    public String getDocno() {
        return docno;
    }

    public int getKeywordCount() {
        return keywordCount;
    }

    public void addInLinks(String inlink) {
        this.inLinks.add(inlink);
    }

    public String getWeburl() {
        return weburl;
    }

    public String getTitle() {
        return title;
    }

    public int getWaveNum() {
        return waveNum;
    }


    public Set<String> getInLinks() {
        return inLinks;
    }

    public static void main(String[] str){

    }

}
