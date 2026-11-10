package bd.hscsolution.student.content;
import android.net.Uri; import java.io.*; import java.net.*;
/** Minimal Java client for the documented public Phase 4 content contract. */
public final class ContentApiClient {
 private final String baseUrl; public ContentApiClient(String baseUrl){this.baseUrl=baseUrl.replaceAll("/$","");}
 public String getQuestion(String questionId,String language) throws IOException {return get("/api/v1/catalog/questions/"+Uri.encode(questionId)+"?language="+Uri.encode(language==null?"bn":language));}
 public String listQuestions(String chapterId,int page,int size) throws IOException {int bounded=Math.max(1,Math.min(size,100));return get("/api/v1/catalog/questions?chapterId="+Uri.encode(chapterId)+"&page="+Math.max(0,page)+"&size="+bounded);}
 private String get(String path) throws IOException {HttpURLConnection c=(HttpURLConnection)new URL(baseUrl+path).openConnection();c.setRequestMethod("GET");c.setRequestProperty("Accept","application/json");c.setConnectTimeout(8000);c.setReadTimeout(10000);int code=c.getResponseCode();try(InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();Reader r=new InputStreamReader(in)){StringBuilder b=new StringBuilder();char[] buf=new char[2048];int n;while((n=r.read(buf))!=-1)b.append(buf,0,n);if(code<200||code>=300)throw new IOException("Content API HTTP "+code+": "+b);return b.toString();}finally{c.disconnect();}}
}
