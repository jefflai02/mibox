package com.minimaltv.launcher;

import android.Manifest;
import android.app.AlertDialog;
import android.widget.EditText;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import android.util.Xml;

/** TV-first dashboard. Public network data; calendar stays on device. No trackers or credentials. */
public class MainActivity extends Activity {
    private final int bg=0xff0B1220, panel=0xff172437, muted=0xff9CB0C8, mint=0xff66E8C8, white=0xffF5F8FD;
    private final Handler ui = new Handler();
    private TextView time, weatherNow, weatherStatus, hourlyTitle, newsStatus, summary, calendarStatus;
    private LinearLayout newsRows, hourlyRows, threeDays, scheduleRows;
    private TextToSpeech speech; private boolean speechReady=false, speaking=false; private TextView voiceButton;
    private final List<String> topHeadlines=new ArrayList<>();
    private String syncUrl=""; private String deviceToken=""; private boolean speakMeetingTitles=false;
    private final List<String> syncedAccounts=new ArrayList<>();
    private String todayForecast="Forecast loading", nextForecast="Three-day forecast loading";
    private volatile String newsNote="News loading", weatherNote="Forecast loading";
    private int highRain=0; private final List<String> meetings=new ArrayList<>();
    private final Runnable tick=new Runnable(){@Override public void run(){ if(time!=null) time.setText(new SimpleDateFormat("EEE, d MMM  •  HH:mm",Locale.ENGLISH).format(new Date()));ui.postDelayed(this,30000); }};
    private int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable shape(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(17));return d;}
    private TextView text(String s,int sp,int color,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(color);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    private LinearLayout vertical(){LinearLayout x=new LinearLayout(this);x.setOrientation(1);return x;}
    private LinearLayout horizontal(){LinearLayout x=new LinearLayout(this);x.setOrientation(0);return x;}
    private void gap(LinearLayout p,int size){p.addView(new View(this),new LinearLayout.LayoutParams(1,dp(size)));}
    private LinearLayout card(){LinearLayout v=vertical();v.setBackground(shape(panel));v.setPadding(dp(20),dp(15),dp(20),dp(17));return v;}
    private void header(LinearLayout p,String title,String subtitle){p.addView(text(title,19,white,true));TextView s=text(subtitle,11,muted,false);LinearLayout.LayoutParams x=new LinearLayout.LayoutParams(-1,dp(25));p.addView(s,x);}
    private void line(LinearLayout p,String s,int sp,int color){TextView t=text(s,sp,color,false);t.setPadding(0,dp(7),0,dp(7));p.addView(t);}
    private void focus(View v){v.setFocusable(true);v.setOnFocusChangeListener((w,b)->{w.setBackground(shape(b?0xff23525E:panel));w.setScaleX(b?1.015f:1);w.setScaleY(b?1.015f:1);});}
    private TextView action(String name,Runnable r){TextView v=text(name,13,white,true);v.setGravity(Gravity.CENTER);v.setPadding(dp(18),dp(9),dp(18),dp(9));v.setBackground(shape(0xff25384B));v.setFocusable(true);v.setOnClickListener(w->r.run());v.setOnFocusChangeListener((w,b)->w.setBackground(shape(b?0xff176A69:0xff25384B)));return v;}
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().getDecorView().setSystemUiVisibility(5894|1024|512);syncUrl=getPreferences(0).getString("sync_url", ""); deviceToken=getPreferences(0).getString("device_token", "");
        build();setupSpeech();ui.post(tick);refreshAll();}
    @Override public void onResume(){super.onResume();loadCalendar();}
    @Override public void onPause(){stopSpeaking();super.onPause();}
    @Override public void onDestroy(){ui.removeCallbacksAndMessages(null);if(speech!=null){speech.stop();speech.shutdown();speech=null;}super.onDestroy();}
    private void build(){
        ScrollView sc=new ScrollView(this);sc.setBackgroundColor(bg);sc.setFillViewport(true);sc.setVerticalScrollBarEnabled(false);
        LinearLayout root=vertical();root.setPadding(dp(34),dp(22),dp(34),dp(25));sc.addView(root);setContentView(sc);
        LinearLayout top=horizontal();top.setGravity(Gravity.CENTER_VERTICAL);root.addView(top);
        LinearLayout greeting=vertical();top.addView(greeting,new LinearLayout.LayoutParams(0,-2,1));greeting.addView(text("GOOD EVENING  ·  JL02 TV",12,mint,true));
        TextView welcome=text("Welcome home.",31,white,true);greeting.addView(welcome);
        time=text("",14,muted,false);time.setPadding(dp(10),0,dp(16),0);top.addView(time);
        voiceButton=action("▶  Listen to briefing",this::toggleSpeech);top.addView(voiceButton);top.addView(action("↻  Refresh",this::refreshAll));top.addView(action("▦  Apps",()->startActivity(new Intent(this,AppsActivity.class))));top.addView(action("☷  Calendars",this::showCalendarOptions));top.addView(action("⚙",()->startActivity(new Intent(Settings.ACTION_SETTINGS))));
        gap(root,18);
        LinearLayout columns=horizontal();columns.setGravity(Gravity.TOP);root.addView(columns);
        LinearLayout news=card();columns.addView(news,new LinearLayout.LayoutParams(0,-2,46));
        header(news,"WORLD IN THE LAST 24 HOURS","AI & technology  •  Markets  •  Malaysia  •  Trade");
        newsStatus=text("Loading latest headlines…",11,mint,false);news.addView(newsStatus);newsRows=vertical();news.addView(newsRows);
        LinearLayout.LayoutParams midp=new LinearLayout.LayoutParams(0,-2,27);midp.leftMargin=dp(12);LinearLayout middle=vertical();columns.addView(middle,midp);
        LinearLayout weather=card();middle.addView(weather);header(weather,"KUALA LUMPUR WEATHER","Today · hourly outlook");
        weatherNow=text("Checking forecast…",26,white,true);weather.addView(weatherNow);
        weatherStatus=text("Connecting to forecast provider",11,muted,false);weather.addView(weatherStatus);
        hourlyTitle=text("NEXT HOURS",11,mint,true);hourlyTitle.setPadding(0,dp(12),0,0);weather.addView(hourlyTitle);
        HorizontalScrollView hsc=new HorizontalScrollView(this);hsc.setHorizontalScrollBarEnabled(false);weather.addView(hsc);hourlyRows=horizontal();hsc.addView(hourlyRows);
        gap(middle,12);LinearLayout forecast=card();middle.addView(forecast);header(forecast,"NEXT 3 DAYS","Daily high / low and rain probability");threeDays=vertical();forecast.addView(threeDays);
        LinearLayout.LayoutParams rightp=new LinearLayout.LayoutParams(0,-2,27);rightp.leftMargin=dp(12);LinearLayout right=vertical();columns.addView(right,rightp);
        LinearLayout day=card();right.addView(day);header(day,"YOUR DAY","Google + Microsoft · securely synced");
        calendarStatus=text("Checking calendar access…",11,muted,false);day.addView(calendarStatus);scheduleRows=vertical();day.addView(scheduleRows);
        day.addView(action("☷  Connect calendars",this::showCalendarOptions));
        gap(right,12);LinearLayout notes=card();right.addView(notes);header(notes,"THINGS TO NOTE","Your daily briefing");summary=text("Gathering weather, news and schedule…",13,white,false);notes.addView(summary);
        gap(root,13);LinearLayout bottom=card();root.addView(bottom);header(bottom,"QUICK LAUNCH","Your installed apps are one click away");
        LinearLayout actions=horizontal();bottom.addView(actions);actions.addView(action("▦  All applications",()->startActivity(new Intent(this,AppsActivity.class))));actions.addView(action("◉  Wi-Fi",()->startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS))));actions.addView(action("⚙  Device settings",()->startActivity(new Intent(Settings.ACTION_SETTINGS))));
        line(root,"Remote: arrow keys to move   •   OK to open   •   Refresh for new headlines and weather   •   No advertising",11,muted);
    }
    private void refreshAll(){newsStatus.setText("Updating feeds…");weatherStatus.setText("Updating forecast…");new Thread(this::fetchNews).start();new Thread(this::fetchWeather).start();loadCalendar();}
    private InputStream open(String url) throws Exception {HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(8500);c.setReadTimeout(9000);c.setRequestProperty("User-Agent","JL02TV/2.2 (Android TV)");c.setInstanceFollowRedirects(true);if(c.getResponseCode()!=200)throw new Exception("HTTP "+c.getResponseCode());return c.getInputStream();}
    private String read(InputStream in)throws Exception{try(InputStream i=in){java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=i.read(b))!=-1){out.write(b,0,n);if(out.size()>650000)break;}return out.toString("UTF-8");}}
    private static class Story {String title,link,category;long published;Story(String t,String l,String c,long p){title=t;link=l;category=c;published=p;}}
    private void fetchNews(){
        List<Story> all=new ArrayList<>();String[][] topics={{"AI and technology","AI artificial intelligence technology"},{"Semiconductors","semiconductors chip supply chain"},{"Malaysia business","Malaysia economy business"},{"Global markets","world markets finance"},{"Trade & supply","Malaysia US trade tariffs electronics"}};
        for(String[] topic:topics){try{
            String q=URLEncoder.encode(topic[1]+" when:1d","UTF-8");String url="https://news.google.com/rss/search?q="+q+"&hl=en-MY&gl=MY&ceid=MY:en";
            XmlPullParser x=Xml.newPullParser();try(InputStream stream=open(url)){x.setInput(stream,"UTF-8");boolean item=false;String tag="",title="",link="",date="";
                for(int e=x.getEventType();e!=XmlPullParser.END_DOCUMENT;e=x.next()){
                    if(e==XmlPullParser.START_TAG){tag=x.getName();if("item".equals(tag)){item=true;title="";link="";date="";}}
                    else if(e==XmlPullParser.TEXT&&item){String s=x.getText();if("title".equals(tag))title+=s;else if("link".equals(tag))link+=s;else if("pubDate".equals(tag))date+=s;}
                    else if(e==XmlPullParser.END_TAG){String end=x.getName();if("item".equals(end)){long ts=0;try{ts=new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",Locale.US).parse(date).getTime();}catch(Exception ignored){}if(ts>System.currentTimeMillis()-86400000L&&title.length()>10&&all.size()<20)all.add(new Story(title,link,topic[0],ts));item=false;}tag="";}
                }
            }
        }catch(Exception ignored){} }
        ui.post(()->{newsRows.removeAllViews();if(all.isEmpty()){newsStatus.setText("Live headlines unavailable · Check internet and retry");line(newsRows,"No cached or invented headlines are displayed.",13,muted);topHeadlines.clear();newsNote="News feeds unavailable";}else{newsStatus.setText("Updated now · news published within 24 hours");int count=0;for(Story s:all){if(count++>=8)break;LinearLayout item=vertical();item.setPadding(dp(5),dp(9),dp(5),dp(11));item.addView(text(s.category.toUpperCase(Locale.US),10,mint,true));item.addView(text(s.title,14,white,true));String when=new SimpleDateFormat("HH:mm",Locale.ENGLISH).format(new Date(s.published));item.addView(text("Published "+when+" · OK to open story",10,muted,false));focus(item);item.setOnClickListener(v->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(s.link)));}catch(Exception ignored){}});newsRows.addView(item);}topHeadlines.clear();for(int i=0;i<Math.min(5,all.size());i++)topHeadlines.add(all.get(i).title);newsNote=all.size()+" recent news headlines available";}updateSummary();});
    }
    private void fetchWeather(){try{
        // KL default coordinates; no location permissions or device tracking necessary.
        String url="https://api.open-meteo.com/v1/forecast?latitude=3.1390&longitude=101.6869&timezone=Asia%2FKuala_Lumpur&current=temperature_2m,relative_humidity_2m,weather_code&hourly=temperature_2m,precipitation_probability,weather_code&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max,weather_code&forecast_days=4";
        JSONObject root=new JSONObject(read(open(url)));JSONObject now=root.getJSONObject("current"),hours=root.getJSONObject("hourly"),days=root.getJSONObject("daily");
        int temperature=(int)Math.round(now.getDouble("temperature_2m"));int humid=now.getInt("relative_humidity_2m");
        JSONArray htime=hours.getJSONArray("time"),htemp=hours.getJSONArray("temperature_2m"),hrain=hours.getJSONArray("precipitation_probability");
        JSONArray dtime=days.getJSONArray("time"),dhi=days.getJSONArray("temperature_2m_max"),dlo=days.getJSONArray("temperature_2m_min"),drain=days.getJSONArray("precipitation_probability_max");
        String today=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());List<String[]> hourList=new ArrayList<>(),dayList=new ArrayList<>();int max=0;
        for(int i=0;i<htime.length();i++){String stamp=htime.getString(i);if(stamp.startsWith(today)){int rain=hrain.optInt(i,0);max=Math.max(max,rain);hourList.add(new String[]{stamp.substring(11),String.valueOf((int)Math.round(htemp.getDouble(i)))+"°",rain+"%"});}}
        for(int i=1;i<Math.min(4,dtime.length());i++)dayList.add(new String[]{dtime.getString(i),Math.round(dhi.getDouble(i))+"° / "+Math.round(dlo.getDouble(i))+"°",drain.optInt(i)+"% rain"});
        final int rain=max;ui.post(()->{highRain=rain;weatherNow.setText(temperature+"°C  ·  "+humid+"% humidity");weatherStatus.setText("Today · hourly precipitation outlook");hourlyRows.removeAllViews();for(String[] h:hourList){LinearLayout col=vertical();col.setPadding(dp(11),dp(8),dp(11),dp(8));col.addView(text(h[0],11,muted,true));col.addView(text(h[1],18,white,true));col.addView(text("☂ "+h[2],11,mint,false));hourlyRows.addView(col);}threeDays.removeAllViews();for(String[] d:dayList){line(threeDays,d[0]+"   "+d[1]+"   ☂ "+d[2],12,white);}todayForecast="Currently "+temperature+" degrees Celsius with "+humid+" percent humidity. Today peak hourly rain chance is "+rain+" percent.";StringBuilder next=new StringBuilder();for(String[] d:dayList)next.append(d[0]).append(": ").append(d[1].replace("°", " degrees ")).append(", ").append(d[2]).append(". ");nextForecast=next.toString();weatherNote=rain>=60?"Possible heavy rain today (up to "+rain+"% hourly probability)":"Today's maximum hourly rain chance: "+rain+"%";updateSummary();});
        }catch(Exception e){ui.post(()->{weatherNow.setText("Forecast unavailable");weatherStatus.setText("Connect to internet, then select Refresh");weatherNote="Weather not updated";todayForecast="Forecast unavailable";nextForecast="Three-day forecast unavailable";updateSummary();});}}
    private void showCalendarOptions(){
        String[] opts={"Connect Google / Outlook (QR)","Set secure sync server URL", "Refresh schedule", "Disconnect an account", "Speak non-private meeting titles: "+(speakMeetingTitles?"ON":"OFF"),"Unlink this TV"};
        new AlertDialog.Builder(this).setTitle("JL02 TV · Calendars").setItems(opts,(d,which)->{
            switch(which){case 0: beginPairing();break;case 1: configureServer();break;case 2:loadCalendar();break;
            case 3:chooseDisconnect();break;case 4:speakMeetingTitles=!speakMeetingTitles;break;case 5:unlinkTV();break;}
        }).setNegativeButton("Close",null).show();
    }
    private void configureServer(){EditText input=new EditText(this);input.setSingleLine();input.setText(syncUrl);input.setHint("https://your-secure-service.example");
        new AlertDialog.Builder(this).setTitle("Secure calendar service HTTPS URL").setView(input).setPositiveButton("Save",(d,w)->{
            String u=input.getText().toString().trim().replaceAll("/+$","");
            if(!u.startsWith("https://")){message("HTTPS is required");return;}
            if(!u.equals(syncUrl)){syncUrl=u;deviceToken="";storeConnection();}loadCalendar();
        }).setNegativeButton("Cancel",null).show();}
    private void storeConnection(){getPreferences(0).edit().putString("sync_url",syncUrl).putString("device_token",deviceToken).apply();}
    private void message(String m){new AlertDialog.Builder(this).setMessage(m).setPositiveButton("OK",null).show();}
    private JSONObject api(String method,String path,JSONObject data) throws Exception{
        java.net.HttpURLConnection c=(java.net.HttpURLConnection)new java.net.URL(syncUrl+path).openConnection();
        c.setRequestMethod(method);c.setConnectTimeout(10000);c.setReadTimeout(15000);
        if(!deviceToken.isEmpty())c.setRequestProperty("Authorization","Bearer "+deviceToken);
        if(data!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");c.getOutputStream().write(data.toString().getBytes("UTF-8"));}
        int status=c.getResponseCode();if(status<200||status>=300)throw new Exception("Server HTTP "+status);
        if(method.equals("DELETE"))return new JSONObject(read(c.getInputStream()));
        return new JSONObject(read(c.getInputStream()));
    }
    private void ensureDevice() throws Exception{if(deviceToken.isEmpty()){
        deviceToken=api("POST","/v1/device",new JSONObject()).getString("device_token");storeConnection();}}
    private void beginPairing(){if(syncUrl.isEmpty()){configureServer();return;}
        new Thread(()->{try{ensureDevice();String link=api("POST","/v1/pair",new JSONObject()).getString("url");ui.post(()->showPairQR(link));}
        catch(Exception e){ui.post(()->message("Could not begin pairing: "+e.getMessage()));}}).start();}
    private void showPairQR(String link){try{
        BitMatrix bits=new MultiFormatWriter().encode(link,BarcodeFormat.QR_CODE,360,360);
        Bitmap bitmap=Bitmap.createBitmap(360,360,Bitmap.Config.ARGB_8888);
        for(int y=0;y<360;y++)for(int x=0;x<360;x++)bitmap.setPixel(x,y,bits.get(x,y)?android.graphics.Color.BLACK:android.graphics.Color.WHITE);
        ImageView image=new ImageView(this);image.setImageBitmap(bitmap);image.setPadding(dp(30),dp(20),dp(30),dp(20));
        new AlertDialog.Builder(this).setTitle("Scan with your phone · expires in 10 minutes").setView(image)
            .setMessage("Choose Google or Microsoft in your phone browser. Then select Refresh schedule on the TV.")
            .setPositiveButton("Refresh schedule",(d,w)->loadCalendar()).setNegativeButton("Close",null).show();
        }catch(Exception e){message("Open on phone: "+link);}}
    private void syncCalendar(){new Thread(()->{try{
        JSONObject r=api("GET","/v1/events",null);JSONArray items=r.getJSONArray("events");List<String> fresh=new ArrayList<>();
        for(int i=0;i<items.length() && i<24;i++){
            JSONObject x=items.getJSONObject(i);String title=x.optString("title","Busy");
            if(x.optBoolean("private"))title="Private appointment";
            fresh.add(x.optString("time")+" · "+title+" ("+x.optString("provider")+")");}
        int connected=r.optInt("connected_accounts");JSONArray errors=r.optJSONArray("errors");
        ui.post(()->{meetings.clear();meetings.addAll(fresh);scheduleRows.removeAllViews();
            calendarStatus.setText(connected+" connected account(s)"+(errors!=null&&errors.length()>0?" · Some calendars failed to sync":""));
            if(fresh.isEmpty())line(scheduleRows,"No appointments today",12,muted);
            else for(String m:fresh)line(scheduleRows,m,12,white);
            updateSummary();});
        }catch(Exception e){ui.post(()->{calendarStatus.setText("Calendar sync unavailable");scheduleRows.removeAllViews();line(scheduleRows,"Check server or select Connect calendars",12,muted);meetings.clear();updateSummary();});}}).start();}
    private void chooseDisconnect(){if(syncUrl.isEmpty()||deviceToken.isEmpty()){message("No server connected");return;}
        new Thread(()->{try{JSONArray a=api("GET","/v1/accounts",null).getJSONArray("accounts");
        String[] names=new String[a.length()];String[] ids=new String[a.length()];for(int i=0;i<a.length();i++){names[i]=a.getJSONObject(i).getString("provider")+" · account "+(i+1);ids[i]=a.getJSONObject(i).getString("id");}
        ui.post(()->new AlertDialog.Builder(this).setTitle("Disconnect calendar")
            .setItems(names,(d,i)->new Thread(()->{try{api("DELETE","/v1/accounts/"+ids[i],null);ui.post(this::loadCalendar);}catch(Exception ex){ui.post(()->message(ex.getMessage()));}}).start())
            .setNegativeButton("Cancel",null).show());
        }catch(Exception e){ui.post(()->message("Could not load accounts"));}}).start();}
    private void unlinkTV(){new AlertDialog.Builder(this).setTitle("Unlink TV?").setMessage("This removes this TV's connected calendars and session from the server.")
        .setPositiveButton("Unlink",(d,w)->new Thread(()->{try{if(!deviceToken.isEmpty())api("DELETE","/v1/device",null);}catch(Exception ignored){}
            ui.post(()->{deviceToken="";storeConnection();meetings.clear();loadCalendar();});}).start()).setNegativeButton("Cancel",null).show();}
    private void requestCalendar(){if(android.os.Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.READ_CALENDAR)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.READ_CALENDAR},50);}else loadCalendar();}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==50)loadCalendar();}
    private void loadCalendar(){if(!syncUrl.isEmpty()){
        if(deviceToken.isEmpty()){calendarStatus.setText("Connect Google or Outlook to begin");scheduleRows.removeAllViews();return;}
        syncCalendar();return;}
        if(android.os.Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.READ_CALENDAR)!=PackageManager.PERMISSION_GRANTED){calendarStatus.setText("Calendar permission not granted");scheduleRows.removeAllViews();line(scheduleRows,"Grant access to show your actual appointments.",12,muted);meetings.clear();updateSummary();return;}
        Calendar start=Calendar.getInstance();start.set(Calendar.HOUR_OF_DAY,0);start.set(Calendar.MINUTE,0);start.set(Calendar.SECOND,0);long from=start.getTimeInMillis();long to=from+86400000L;
        List<String> temp=new ArrayList<>();try{Uri.Builder builder=CalendarContract.Instances.CONTENT_URI.buildUpon();android.content.ContentUris.appendId(builder,from);android.content.ContentUris.appendId(builder,to);try(Cursor c=getContentResolver().query(builder.build(),new String[]{CalendarContract.Instances.TITLE,CalendarContract.Instances.BEGIN,CalendarContract.Instances.ALL_DAY},null,null,CalendarContract.Instances.BEGIN+" ASC")){if(c!=null){while(c.moveToNext()&&temp.size()<8){String title=c.getString(0);long stamp=c.getLong(1);boolean allDay=c.getInt(2)==1;String t=allDay?"ALL DAY":new SimpleDateFormat("HH:mm",Locale.ENGLISH).format(new Date(stamp));temp.add(t+"  ·  "+title);}}}}catch(Exception ignored){}
        meetings.clear();meetings.addAll(temp);scheduleRows.removeAllViews();if(temp.isEmpty()){calendarStatus.setText("No appointments in calendars on this TV");line(scheduleRows,"If your Google Calendar doesn't appear, enable calendar sync for this device.",12,muted);}else{calendarStatus.setText(temp.size()+" event(s) today");for(String m:temp)line(scheduleRows,m,12,white);}updateSummary();
    }

    private void setupSpeech(){
        voiceButton.setText("◌  Preparing voice…");
        speech=new TextToSpeech(this,status->{
            if(status==TextToSpeech.SUCCESS && speech!=null){
                int result=speech.setLanguage(Locale.ENGLISH);
                if(result!=TextToSpeech.LANG_MISSING_DATA && result!=TextToSpeech.LANG_NOT_SUPPORTED){
                    speechReady=true;speech.setSpeechRate(0.92f);speech.setPitch(1.0f);
                    speech.setOnUtteranceProgressListener(new UtteranceProgressListener(){
                        @Override public void onStart(String id){ui.post(()->{speaking=true;voiceButton.setText("■  Stop briefing");});}
                        @Override public void onDone(String id){ui.post(()->{speaking=false;voiceButton.setText("▶  Listen to briefing");});}
                        @Override public void onError(String id){ui.post(()->{speaking=false;voiceButton.setText("▶  Voice unavailable");});}
                    });
                    ui.post(()->voiceButton.setText("▶  Listen to briefing"));return;
                }
            }
            ui.post(()->voiceButton.setText("Voice engine unavailable"));
        });
    }
    private void toggleSpeech(){
        if(speaking){stopSpeaking();return;}
        if(!speechReady || speech==null){voiceButton.setText("Install/enable Android Text-to-Speech");return;}
        String narration=createNarration();
        speaking=true;voiceButton.setText("■  Stop briefing");
        // QUEUE_FLUSH prevents multiple overlapping dashboard readings.
        int result=speech.speak(narration,TextToSpeech.QUEUE_FLUSH,null,"jl02_tv_daily_briefing");
        if(result==TextToSpeech.ERROR){speaking=false;voiceButton.setText("▶  Voice unavailable");}
    }
    private void stopSpeaking(){
        if(speech!=null)speech.stop();speaking=false;
        if(voiceButton!=null && speechReady)voiceButton.setText("▶  Listen to briefing");
    }
    private String createNarration(){
        StringBuilder b=new StringBuilder();
        String greeting=new SimpleDateFormat("EEEE, MMMM d",Locale.ENGLISH).format(new Date());
        b.append("Welcome home. Here's your personal dashboard briefing for ").append(greeting).append(". ");
        b.append("Weather in Kuala Lumpur. ").append(todayForecast).append(" ");
        b.append("Your next three days. ").append(nextForecast).append(" ");
        b.append("Your schedule today. ");
        if(meetings.isEmpty())b.append("No calendar events are available on this TV. Calendar access or sync may be required. ");
        else{b.append(meetings.size()).append(" appointments. ");for(String event:meetings)b.append(speakMeetingTitles?event:event.split(" · ")[0]+". Scheduled appointment").append(". ");}
        b.append("Global headlines from the past 24 hours. ");
        if(topHeadlines.isEmpty())b.append("Recent news headlines are currently unavailable. ");
        else for(int i=0;i<topHeadlines.size();i++)b.append("Headline ").append(i+1).append(". ").append(topHeadlines.get(i)).append(". ");
        b.append("Things to note. ").append(weatherNote).append(". ").append(newsNote).append(". ");
        b.append("That's your briefing. Have a great day.");
        return b.toString();
    }
    private void updateSummary(){if(summary==null)return;StringBuilder s=new StringBuilder();s.append("☁ WEATHER\n").append(weatherNote).append("\n\n");s.append("◷ SCHEDULE\n");if(meetings.isEmpty())s.append("No synced events found for today.");else{s.append(meetings.size()).append(" events today.\n");for(int i=0;i<Math.min(3,meetings.size());i++)s.append("• ").append(meetings.get(i)).append("\n");}s.append("\n◈ NEWS\n").append(newsNote);summary.setText(s.toString());}
}
