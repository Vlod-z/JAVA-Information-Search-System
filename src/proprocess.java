import java.io.*;
import java.util.*;

import net.paoding.analysis.analyzer.PaodingAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.textmining.text.extraction.WordExtractor;

public class proprocess {
    File root = new File("D:\\Documents\\ClassFiles\\Java\\xinxijiansuo\\src\\roots");
    File[] docs = root.listFiles();
    Map<String, Integer>[] map = new Map[docs.length];
    Map<String,Integer> df=new HashMap<>();//全文档集词表及频率
    ArrayList<ArrayList<String>> index = new ArrayList<>();//倒排索引
    Map<String,Double>[] vec= new Map[docs.length];
    double[] length=new double[docs.length];
    double[] searchScore = new double[docs.length];
    String[] searchtarget= new String[docs.length];
    int[] searchrank=new int[docs.length];
    String[] doctitle = new String[docs.length];
    String[] doctime = new String[docs.length];
    String[] docauthor = new String[docs.length];

    public void printout(){
        System.out.println("=================================查询结果==================================");
        for(int i=0;i<docs.length;i++){
            if(Math.abs(searchScore[searchrank[i]])>0.000001){
                System.out.println("filename:"+docs[searchrank[i]].getName()+" | title:"+doctitle[searchrank[i]]+
                        " | author:"+docauthor[searchrank[i]]+" | "+doctime[searchrank[i]]+" | url:"+docs[searchrank[i]].getPath()+
                        " | score:"+searchScore[searchrank[i]]+" | maintoken:"+searchtarget[searchrank[i]].trim());
            }
        }
    }

    public void getRanking(){
        for(int i=0;i<docs.length;i++){
            searchrank[i]=i;
        }
        for(int i=docs.length-1;i>0;i--){
            for(int j=0;j<i;j++){
                if(searchScore[searchrank[j]]<searchScore[searchrank[j+1]]){
                    int temp=searchrank[j];
                    searchrank[j]=searchrank[j+1];
                    searchrank[j+1]=temp;
                }
            }
        }
    }

    public void looking(String search){
        Analyzer analyzer = new PaodingAnalyzer();
        List<String> searchTokenList = new ArrayList<>();
        tokenlistMake(search,analyzer,searchTokenList);
        for(String s:searchTokenList){
            for(ArrayList<String> in:index){
                if(s.equals(in.get(0))){
                    for(int i=1;i<in.size();i++){
                        int target=Integer.parseInt(in.get(i));
                        searchScore[target]+=vec[target].get(s)/length[target];
                        if(searchtarget[target]==null){
                            searchtarget[target]=s+"/";
                        }
                        else
                            searchtarget[target]+=s+"/";
                    }
                }
            }
        }
        getRanking();
        printout();
    }

    public void vectorspace(){
        for(int i=0;i<docs.length;i++){
            Iterator iter = map[i].entrySet().iterator();
            Map<String,Double> tempmap=new HashMap<>();
            while (iter.hasNext()){
                double weight;
                Map.Entry enter = (Map.Entry) iter.next();
                String word = (String) enter.getKey();
                if(df.get(word)<=docs.length/2){
                    weight=(Math.log10((double)(int)enter.getValue()+1))*(Math.log10((double)docs.length/(double)df.get(word)));//tf*idf
                    length[i]+=weight*weight;
                    if(tempmap == null){
                        tempmap.put(word,weight);
                    }else{
                        tempmap.put(word, weight);
                    }
                }
            }
            vec[i]=tempmap;
            length[i]=Math.sqrt(length[i]);
        }
    }

    public void makeIndex() throws Exception{//倒排索引创建
        for(int i=0;i<docs.length;i++){
            Iterator iter = map[i].entrySet().iterator();
            while(iter.hasNext()){
                Map.Entry enter = (Map.Entry) iter.next();
                String word = (String) enter.getKey();
                if(df.get(word)<=docs.length/2){
                    int flag=0;
                    if(index.size()!=0){
                        for(ArrayList<String> s:index){
                            String a=s.get(0);
                            if(a.equals(word)){
                                flag=1;
                                s.add(Integer.toString(i));
                            }
                        }
                    }
                    if(flag==0){
                        ArrayList<String> temp=new ArrayList<>();
                        temp.add(word);
                        temp.add(Integer.toString(i));
                        index.add(temp);
                    }
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        for(ArrayList<String> f:index){
            for(String s:f){
                sb.append(s+"/");
            }
            sb.append("\r\n");
        }
        File indexfile = new File("D:\\Documents\\ClassFiles\\Java\\xinxijiansuo\\src\\index.txt");
        if (!indexfile.exists()) {
           indexfile.createNewFile();
        }
        FileOutputStream fo1 = new FileOutputStream(indexfile);
        OutputStreamWriter os1 = new OutputStreamWriter(fo1);
        BufferedWriter bw1 = new BufferedWriter(os1);
        bw1.write(sb.toString().trim());
        bw1.close();
        os1.close();
        fo1.close();
    }

    public String dtw (File file,int docid) throws Exception{//读取文档内容存进一个字符串
        StringBuilder text = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(file));//构造一个BufferedReader类来读取文件
        String s = null;
        int i=0;
        while((s = br.readLine())!=null){//使用readLine方法，一次读一行
            if(i==0){
                doctitle[docid]=s;
            }
            else if(i==1){
                docauthor[docid]=s;
            }
            else if(i==2){
                doctime[docid]=s;
            }
            else if(i==3){
                doctime[docid]+=" "+s;
            }
            text.append(s);
            i++;
        }
        br.close();
        return text.toString().trim();
    }



    public void makeTerms() {//循环读入文档到生成每篇文档词汇词频表，并汇总生成全文档集词汇词频表（某个词汇在几篇文档中出现）
        Analyzer analyzer = new PaodingAnalyzer();
        try {
            for (int i = 0; i < docs.length; i++) {
                String docStr = dtw(docs[i],i);
                List<String> tokenList = new ArrayList<>();
                tokenlistMake(docStr,analyzer,tokenList);
                Map<String, Integer> tfmap=count(tokenList);
                map[i]=tfmap;

                StringBuilder sb = new StringBuilder();

                Iterator iter = tfmap.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry enter = (Map.Entry) iter.next();
                    String word = (String) enter.getKey();
                    int wordNum = (int) enter.getValue();
                    if(df == null){
                        df.put(word, 1);
                    }else{
                        if(df.get(word)==null){
                            df.put(word, 1);
                        }
                        else{
                            int num = df.get(word);
                            num++;
                            df.put(word, num);
                        }
                    }
                    sb.append(word + "/" + wordNum + "\r\n");
                }

                //每一篇文档成成一个词项文本
                String docId = docs[i].getName();
                docId = docId.substring(0, docId.length() - 4);
                String fileStr = "D:\\Documents\\ClassFiles\\Java\\xinxijiansuo\\src\\terms\\" + docId + ".txt";
                File index = new File(fileStr);
                if (!index.exists()) {
                    index.createNewFile();
                }
                FileOutputStream fo = new FileOutputStream(index);
                OutputStreamWriter os = new OutputStreamWriter(fo);
                BufferedWriter bw = new BufferedWriter(os);
                bw.write(sb.toString().trim());

                bw.close();
                os.close();
                fo.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void tokenlistMake(String dstr,Analyzer analyzer,List<String> tokenlist){//将文档字符串分词生成词汇表
        try{
            TokenStream ts = analyzer.tokenStream("", new StringReader(dstr));
            Token token;
            while ((token = ts.next()) != null) {
                tokenlist.add(token.termText());
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static Map count(List list){//对每篇文档进行词频计数
        Map<String,Integer> mapp = new HashMap<>();
        String temp = "";
        for(int i=0 ; i<list.size();i++){
            temp = (String) list.get(i);
            if(mapp == null){
                mapp.put(temp, 1);
            }else{
                if(mapp.get(temp)==null){
                    mapp.put(temp, 1);
                }
                else{
                    int num = mapp.get(temp);
                    num++;
                    mapp.put(temp, num);
                }
            }
        }
        return mapp;
    }
}
