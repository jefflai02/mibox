package com.minimaltv.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.security.KeyStore;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONArray;
import org.json.JSONObject;

public class MicrosoftCalendar {
  public static class Result { public List<String> events=new ArrayList<>(); public String error; }
  public interface Done {void accept(Result r);}
  private final Activity a;
  private final Handler main=new Handler(Looper.getMainLooper());
  private static final String AUTH="https://login.microsoftonline.com/organizations/oauth2/v2.0/";
  private static final String SCOPES="offline_access Calendars.ReadBasic";
  private final SharedPreferences prefs;
  private volatile boolean canceled=false;
  MicrosoftCalendar(Activity activity){a=activity;prefs=a.getSharedPreferences("ms_auth",0);}
  boolean isConfigured(){return BuildConfig.MS_CLIENT_ID!=null && BuildConfig.MS_CLIENT_ID.matches("[0-9a-fA-F-]{36}");}
  boolean isConnected(){return !getRefresh().isEmpty();}
  void disconnect(){canceled=true;prefs.edit().remove("refresh").apply();}
  private String encrypt(String input)throws Exception{
    KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
    if(!store.containsAlias("jl02_ms_token")){
      KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
      gen.init(new KeyGenParameterSpec.Builder("jl02_ms_token",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build());gen.generateKey();
    }
    SecretKey key=(SecretKey)store.getKey("jl02_ms_token",null);
    Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key);
    byte[] ciphertext=c.doFinal(input.getBytes("UTF-8"));
    return Base64.encodeToString(c.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(ciphertext,Base64.NO_WRAP);
  }
  private String decrypt(String encoded)throws Exception{
    String[] halves=encoded.split(":",2);if(halves.length!=2)return "";
    KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
    SecretKey key=(SecretKey)store.getKey("jl02_ms_token",null);
    Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
    c.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,Base64.decode(halves[0],Base64.NO_WRAP)));
    return new String(c.doFinal(Base64.decode(halves[1],Base64.NO_WRAP)),"UTF-8");
  }
  private String getRefresh(){try{return decrypt(prefs.getString("refresh",""));}catch(Exception e){return "";}}
  private void saveRefresh(String s)throws Exception {prefs.edit().putString("refresh",encrypt(s)).apply();}
  private JSONObject request(String url,String payload)throws Exception {
    HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(12000);c.setReadTimeout(13000);
    c.setRequestProperty("Accept","application/json");
    if(payload!=null){c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/x-www-form-urlencoded");try(OutputStream out=c.getOutputStream()){out.write(payload.getBytes("UTF-8"));}}
    int status=c.getResponseCode();try(InputStream in=(status>=200&&status<300)?c.getInputStream():c.getErrorStream()){
      ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] tmp=new byte[4096];int len;while((len=in.read(tmp))!=-1){b.write(tmp,0,len);if(b.size()>400000)break;}
      JSONObject j=new JSONObject(b.toString("UTF-8"));if(status>=400&&!j.has("error"))j.put("error","HTTP "+status);return j;
    }finally{c.disconnect();}
  }
  private String form(String... fields)throws Exception {
    StringBuilder sb=new StringBuilder();for(int i=0;i<fields.length;i+=2){if(sb.length()>0)sb.append('&');sb.append(URLEncoder.encode(fields[i],"UTF-8")).append('=').append(URLEncoder.encode(fields[i+1],"UTF-8"));}return sb.toString();
  }
  void signIn(Runnable success){
    if(!isConfigured())return;canceled=false;
    new Thread(()->{try{
      JSONObject d=request(AUTH+"devicecode",form("client_id",BuildConfig.MS_CLIENT_ID,"scope",SCOPES));
      if(d.has("error"))throw new Exception(d.optString("error_description",d.optString("error")));
      String code=d.getString("device_code"),userCode=d.getString("user_code"),url=d.optString("verification_uri","https://microsoft.com/devicelogin");
      int delay=Math.max(5,d.optInt("interval",5));long until=System.currentTimeMillis()+Math.min(900,d.optInt("expires_in",900))*1000L;
      main.post(()->new AlertDialog.Builder(a).setTitle("Microsoft 365 · Sign in using your phone")
        .setMessage("1. On your phone open:\n"+url+"\n\n2. Enter this code:\n\n"+userCode+"\n\n3. Sign in and approve read-only calendar access.\n\nReturn to the TV after approval.")
        .setPositiveButton("Keep waiting",(x,y)->{}).setNegativeButton("Cancel",(x,y)->canceled=true).show());
      while(!canceled&&System.currentTimeMillis()<until){Thread.sleep(delay*1000L);
        JSONObject token=request(AUTH+"token",form("grant_type","urn:ietf:params:oauth:grant-type:device_code","client_id",BuildConfig.MS_CLIENT_ID,"device_code",code));
        if(token.has("access_token")) {String refresh=token.optString("refresh_token","");if(refresh.isEmpty())throw new Exception("Microsoft did not issue a refresh token.");saveRefresh(refresh);main.post(()->{new AlertDialog.Builder(a).setTitle("Microsoft 365 connected").setMessage("Your work calendar is ready.").setPositiveButton("OK",null).show();success.run();});return;}
        String err=token.optString("error");if(err.equals("authorization_pending"))continue;if(err.equals("slow_down")){delay+=5;continue;}
        throw new Exception(token.optString("error_description",err));
      }
      if(!canceled)throw new Exception("Sign-in code expired. Try again.");
    }catch(Exception ex){main.post(()->showError(ex.getMessage()));}}).start();
  }
  private void showError(String msg){new AlertDialog.Builder(a).setTitle("Microsoft 365 connection").setMessage(msg==null?"Connection failed":msg).setPositiveButton("OK",null).show();}
  void loadToday(Done done){new Thread(()->{Result result=new Result();try{
      String refresh=getRefresh();if(refresh.isEmpty())throw new Exception("Please sign in again.");
      JSONObject token=request(AUTH+"token",form("grant_type","refresh_token","client_id",BuildConfig.MS_CLIENT_ID,"refresh_token",refresh,"scope",SCOPES));
      if(token.has("error"))throw new Exception("Microsoft sign-in expired or requires company approval: "+token.optString("error_description",token.optString("error")));
      if(token.has("refresh_token"))saveRefresh(token.getString("refresh_token"));
      String access=token.getString("access_token");
      Calendar start=Calendar.getInstance();start.set(Calendar.HOUR_OF_DAY,0);start.set(Calendar.MINUTE,0);start.set(Calendar.SECOND,0);start.set(Calendar.MILLISECOND,0);
      Calendar end=(Calendar)start.clone();end.add(Calendar.DAY_OF_YEAR,1);
      SimpleDateFormat iso=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX",Locale.US);
      String q="startDateTime="+URLEncoder.encode(iso.format(start.getTime()),"UTF-8")+"&endDateTime="+URLEncoder.encode(iso.format(end.getTime()),"UTF-8")+"&$select=subject,start,end,isPrivate,sensitivity&$top=60";
      HttpURLConnection c=(HttpURLConnection)new URL("https://graph.microsoft.com/v1.0/me/calendarView?"+q).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(14000);c.setRequestProperty("Authorization","Bearer "+access);c.setRequestProperty("Prefer","outlook.timezone=\""+TimeZone.getDefault().getID()+"\"");
      int status=c.getResponseCode();try(InputStream in=status==200?c.getInputStream():c.getErrorStream()){
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){out.write(buf,0,n);if(out.size()>500000)break;}
        JSONObject data=new JSONObject(out.toString("UTF-8"));if(status!=200)throw new Exception("Microsoft Graph HTTP "+status+": "+(data.optJSONObject("error")!=null?data.optJSONObject("error").optString("message","Permission denied"):"Permission denied"));
        JSONArray list=data.optJSONArray("value");if(list!=null)for(int i=0;i<list.length() && result.events.size()<8;i++){
          JSONObject event=list.getJSONObject(i);if(event.optBoolean("isPrivate")||"private".equalsIgnoreCase(event.optString("sensitivity")))continue;
          String value=event.optJSONObject("start").optString("dateTime","");String when=value.length()>=16?value.substring(11,16):"--:--";
          String title=event.optString("subject","Meeting").replace('\n',' ');if(title.length()>75)title=title.substring(0,75)+"…";
          result.events.add(when+"  ·  "+title);
        }
      }finally{c.disconnect();}
    }catch(Exception ex){result.error=ex.getMessage();}main.post(()->done.accept(result));}).start();}
}
