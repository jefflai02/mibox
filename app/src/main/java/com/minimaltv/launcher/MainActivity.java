package com.minimaltv.launcher;

import android.Manifest;
import android.app.AlertDialog;
import android.widget.EditText;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.view.KeyEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import android.media.MediaPlayer;
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
    private final int bg=0xff080A10, panel=0xff172437, muted=0xff9CB0C8, mint=0xffFFD900, white=0xffF5F8FD;
    private final Handler ui = new Handler();
    private TextView time, weatherNow, weatherStatus, hourlyTitle, newsStatus, summary, calendarStatus;
    private LinearLayout newsRows, hourlyRows, threeDays, scheduleRows, favoriteRow, appRow;
    private TextToSpeech speech; private boolean speechReady=false, speaking=false; private TextView voiceButton;
    private MediaPlayer naturalPlayer; private java.io.File audioCache;
    private final List<String> topHeadlines=new ArrayList<>();
    private String syncUrl=""; private String deviceToken=""; private boolean speakMeetingTitles=false;
    private final List<String> syncedAccounts=new ArrayList<>();
    private String todayForecast="Forecast loading", nextForecast="Three-day forecast loading";
    private volatile String newsNote="News loading", weatherNote="Forecast loading";
    private final List<TVApp> installedApps=new ArrayList<>();
    private SharedPreferences favoritePrefs; private MicrosoftCalendar microsoft;
    private static class TVApp {String pkg,title; Drawable icon; Drawable banner; Intent intent;}
    private int highRain=0; private final List<String> meetings=new ArrayList<>();
    private final Runnable tick=new Runnable(){@Override public void run(){ if(time!=null) time.setText(new SimpleDateFormat("EEE, d MMM  •  HH:mm",Locale.ENGLISH).format(new Date()));ui.postDelayed(this,30000); }};
    private float uiScale(){float wd=getResources().getDisplayMetrics().widthPixels/getResources().getDisplayMetrics().density;return Math.min(1f,Math.max(.64f,wd/1280f));}
    private int dp(int x){return (int)(x*uiScale()*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable shape(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(17));return d;}
    private TextView text(String s,int sp,int color,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp*uiScale());v.setTextColor(color);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    private LinearLayout vertical(){LinearLayout x=new LinearLayout(this);x.setOrientation(1);return x;}
    private LinearLayout horizontal(){LinearLayout x=new LinearLayout(this);x.setOrientation(0);return x;}
    private void gap(LinearLayout p,int size){p.addView(new View(this),new LinearLayout.LayoutParams(1,dp(size)));}
    private LinearLayout card(){LinearLayout v=vertical();v.setBackground(shape(panel));v.setPadding(dp(20),dp(15),dp(20),dp(17));return v;}
    private void header(LinearLayout p,String title,String subtitle){p.addView(text(title,19,white,true));TextView s=text(subtitle,11,muted,false);LinearLayout.LayoutParams x=new LinearLayout.LayoutParams(-1,dp(25));p.addView(s,x);}
    private void line(LinearLayout p,String s,int sp,int color){TextView t=text(s,sp,color,false);t.setPadding(0,dp(7),0,dp(7));p.addView(t);}
    private void focus(View v){v.setFocusable(true);v.setOnFocusChangeListener((w,b)->{w.setBackground(shape(b?0xff23525E:panel));w.setScaleX(b?1.015f:1);w.setScaleY(b?1.015f:1);});}
    private TextView action(String name,Runnable r){TextView v=text(name,13,white,true);v.setGravity(Gravity.CENTER);v.setPadding(dp(18),dp(9),dp(18),dp(9));v.setBackground(shape(0xff25384B));v.setFocusable(true);v.setOnClickListener(w->r.run());v.setOnFocusChangeListener((w,b)->w.setBackground(shape(b?0xff176A69:0xff25384B)));return v;}
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().getDecorView().setSystemUiVisibility(5894|1024|512);syncUrl=BuildConfig.CALENDAR_SERVICE_URL.trim().replaceAll("/+$", ""); deviceToken=getPreferences(0).getString("device_token", "");
        favoritePrefs=getSharedPreferences("home",MODE_PRIVATE);microsoft=new MicrosoftCalendar(this);build();setupSpeech();ui.post(tick);refreshAll();}
    @Override public void onResume(){super.onResume();if(favoritePrefs!=null)renderApps();loadCalendar();}
    @Override public void onPause(){stopSpeaking();super.onPause();}
    @Override public void onDestroy(){ui.removeCallbacksAndMessages(null);if(speech!=null){speech.stop();speech.shutdown();speech=null;} releaseNatural(); if(activeTransfer!=null)activeTransfer.close(); super.onDestroy();}
    private void build(){
        ScrollView sc=new ScrollView(this);sc.setBackgroundColor(bg);sc.setFillViewport(true);sc.setVerticalScrollBarEnabled(false);
        LinearLayout root=vertical();root.setPadding(dp(26),dp(14),dp(26),dp(16));sc.addView(root);setContentView(sc);
        LinearLayout top=horizontal();top.setGravity(Gravity.CENTER_VERTICAL);root.addView(top,new LinearLayout.LayoutParams(-1,dp(84)));
        ImageView brand=new ImageView(this);brand.setImageResource(R.drawable.jl02_logo);brand.setScaleType(ImageView.ScaleType.FIT_CENTER);LinearLayout.LayoutParams bl=new LinearLayout.LayoutParams(dp(106),dp(75));bl.rightMargin=dp(20);top.addView(brand,bl);
        LinearLayout greet=vertical();top.addView(greet,new LinearLayout.LayoutParams(0,-2,1));greet.addView(text("JL02 TV  •  YOUR DAILY BRIEFING",12,mint,true));greet.addView(text("Welcome home.",30,white,true));
        time=text("",12,muted,false);time.setPadding(dp(4),0,dp(8),0);top.addView(time);
        voiceButton=action("▶  Listen to briefing",this::toggleSpeech);top.addView(voiceButton);top.addView(action("↻  Refresh",this::refreshAll));top.addView(action("▦  Calendars",this::showCalendarOptions));top.addView(action("⚙",()->startActivity(new Intent(Settings.ACTION_SETTINGS))));
        gap(root,8);
        LinearLayout favSection=card();root.addView(favSection,new LinearLayout.LayoutParams(-1,dp(175)));
        header(favSection,"FAVORITE APPS","");favoriteRow=horizontal();favSection.addView(favoriteRow,new LinearLayout.LayoutParams(-1,dp(118)));
        gap(root,9);
        LinearLayout duo=horizontal();duo.setGravity(Gravity.TOP);root.addView(duo,new LinearLayout.LayoutParams(-1,dp(425)));
        LinearLayout weather=card();duo.addView(weather,new LinearLayout.LayoutParams(0,-1,58));
        header(weather,"☀  WEATHER · KUALA LUMPUR","Hourly outlook and next three days");
        weatherNow=text("Loading weather…",28,white,true);weather.addView(weatherNow);
        TextView sky=text("☁  ☀  ☂",23,mint,true);sky.setGravity(Gravity.CENTER);weather.addView(sky,new LinearLayout.LayoutParams(-1,dp(27)));
        android.animation.ObjectAnimator pulse=android.animation.ObjectAnimator.ofFloat(sky,"alpha",.68f,1f);pulse.setDuration(2500);pulse.setRepeatCount(android.animation.ValueAnimator.INFINITE);pulse.setRepeatMode(android.animation.ValueAnimator.REVERSE);pulse.start();
        weatherStatus=text("Updating forecast",10,muted,false);weather.addView(weatherStatus);
        hourlyTitle=text("NEXT HOURS",10,mint,true);weather.addView(hourlyTitle);
        HorizontalScrollView hsc=new HorizontalScrollView(this);hsc.setHorizontalScrollBarEnabled(false);weather.addView(hsc,new LinearLayout.LayoutParams(-1,dp(91)));hourlyRows=horizontal();hsc.addView(hourlyRows);
        threeDays=horizontal();weather.addView(threeDays,new LinearLayout.LayoutParams(-1,dp(100)));
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,-1,42);rp.leftMargin=dp(10);LinearLayout calendar=card();duo.addView(calendar,rp);
        header(calendar,"▦  CALENDAR & SCHEDULE","Microsoft 365 · direct phone sign-in");
        LinearLayout calInner=horizontal();calendar.addView(calInner,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout pair=vertical();pair.setPadding(dp(8),dp(5),dp(12),dp(4));calInner.addView(pair,new LinearLayout.LayoutParams(0,-1,45));
        pair.addView(text("Work calendar",16,white,true));
        line(pair,microsoft.isConfigured()?"Sign in on your phone · no NAS":"Microsoft app ID not configured",11,muted);
        TextView connect=action("▦  Microsoft 365 sign-in",this::connectMicrosoft);pair.addView(connect);
        line(pair,"Work calendar read-only · private events hidden",10,muted);
        LinearLayout agenda=vertical();calInner.addView(agenda,new LinearLayout.LayoutParams(0,-1,55));
        agenda.addView(text("TODAY'S SCHEDULE",13,mint,true));calendarStatus=text("Checking calendars…",10,muted,false);agenda.addView(calendarStatus);
        ScrollView listScroll=new ScrollView(this);listScroll.setVerticalScrollBarEnabled(false);agenda.addView(listScroll,new LinearLayout.LayoutParams(-1,0,1));scheduleRows=vertical();listScroll.addView(scheduleRows);
        gap(root,9);
        LinearLayout news=card();root.addView(news,new LinearLayout.LayoutParams(-1,dp(205)));
        LinearLayout nh=horizontal();nh.setGravity(Gravity.CENTER_VERTICAL);news.addView(nh);nh.addView(text("▣  LATEST NEWS",19,white,true),new LinearLayout.LayoutParams(0,dp(27),1));nh.addView(action("View more  ›",()->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://news.google.com/home?hl=en-MY&gl=MY&ceid=MY:en")));}catch(Exception ignored){}}));
        newsStatus=text("Updating latest headlines…",10,muted,false);news.addView(newsStatus,new LinearLayout.LayoutParams(-1,dp(21)));
        newsRows=horizontal();news.addView(newsRows,new LinearLayout.LayoutParams(-1,dp(126)));
        gap(root,9);
        LinearLayout bottom=card();root.addView(bottom,new LinearLayout.LayoutParams(-1,dp(103)));
        LinearLayout strip=horizontal();strip.setGravity(Gravity.CENTER_VERTICAL);bottom.addView(strip,new LinearLayout.LayoutParams(-1,-1));
        TextView label=text("▦  ALL APPS",17,white,true);strip.addView(label,new LinearLayout.LayoutParams(dp(150),-1));
        appRow=horizontal();strip.addView(appRow,new LinearLayout.LayoutParams(0,-1,1));
        renderApps();
    }
    private void refreshAll(){newsStatus.setText("Updating feeds…");weatherStatus.setText("Updating forecast…");new Thread(this::fetchNews).start();new Thread(this::fetchWeather).start();loadCalendar();}
    private InputStream open(String url) throws Exception {HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(8500);c.setReadTimeout(9000);c.setRequestProperty("User-Agent","JL02TV/2.2 (Android TV)");c.setInstanceFollowRedirects(true);if(c.getResponseCode()!=200)throw new Exception("HTTP "+c.getResponseCode());return c.getInputStream();}
    private String read(InputStream in)throws Exception{try(InputStream i=in){java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=i.read(b))!=-1){out.write(b,0,n);if(out.size()>650000)break;}return out.toString("UTF-8");}}
    private static class Story {String title,link,category,image="";long published;Story(String t,String l,String c,long p){title=t;link=l;category=c;published=p;}}
    private void fetchNews(){
        List<Story> all=new ArrayList<>();String[][] topics={{"Technology","https://feeds.bbci.co.uk/news/technology/rss.xml"},{"Business","https://feeds.bbci.co.uk/news/business/rss.xml"},{"World","https://feeds.bbci.co.uk/news/world/rss.xml"},{"Asia","https://feeds.bbci.co.uk/news/world/asia/rss.xml"}};
        for(String[] topic:topics){try{
            String url=topic[1];
            XmlPullParser x=Xml.newPullParser();try(InputStream stream=open(url)){x.setInput(stream,"UTF-8");boolean item=false;String tag="",title="",link="",date="",picture="";
                for(int e=x.getEventType();e!=XmlPullParser.END_DOCUMENT;e=x.next()){
                    if(e==XmlPullParser.START_TAG){tag=x.getName();if(item && ("thumbnail".equals(tag)||"content".equals(tag)||"enclosure".equals(tag))){String candidate=x.getAttributeValue(null,"url");String type=x.getAttributeValue(null,"type");if(candidate!=null && candidate.startsWith("https://") && (type==null || type.startsWith("image/")))picture=candidate;}if("item".equals(tag)){item=true;title="";link="";date="";picture="";}}
                    else if(e==XmlPullParser.TEXT&&item){String s=x.getText();if("title".equals(tag))title+=s;else if("link".equals(tag))link+=s;else if("pubDate".equals(tag))date+=s;}
                    else if(e==XmlPullParser.END_TAG){String end=x.getName();if("item".equals(end)){long ts=0;try{ts=new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",Locale.US).parse(date).getTime();}catch(Exception ignored){}if(ts>System.currentTimeMillis()-86400000L&&title.length()>10&&all.size()<32){Story story=new Story(title,link,topic[0],ts);story.image=picture;all.add(story);}item=false;}tag="";}
                }
            }
        }catch(Exception ignored){} }
        ui.post(()->{
            newsRows.removeAllViews();topHeadlines.clear();
            if(all.isEmpty()){
                newsStatus.setText("Latest articles unavailable · Check network and tap Refresh");
                line(newsRows,"No live articles are available. No simulated headlines are shown.",12,muted);newsNote="News unavailable";
            } else {
                all.sort((a,b)->Long.compare(b.published,a.published));newsStatus.setText("Last 24 hours  ·  BBC newsroom thumbnails where available");
                for(int i=0;i<Math.min(4,all.size());i++){
                    Story s=all.get(i);topHeadlines.add(s.title);
                    LinearLayout tile=vertical();tile.setPadding(dp(6),dp(4),dp(6),dp(3));tile.setBackground(shape(0xff1C2B3A));
                    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);lp.rightMargin=dp(7);newsRows.addView(tile,lp);
                    ImageView thumb=new ImageView(this);thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);thumb.setBackground(shape(0xff293B52));thumb.setContentDescription("News image, when provided by publisher");
                    thumb.setImageDrawable(newsPlaceholder());
                    tile.addView(thumb,new LinearLayout.LayoutParams(-1,dp(67)));
                    if(s.image!=null&&s.image.startsWith("https://"))loadThumbnail(s.image,thumb); else loadOpenGraphImage(s.link,thumb);
                    TextView headline=text(s.title,12,white,true);headline.setMaxLines(2);headline.setEllipsize(TextUtils.TruncateAt.END);tile.addView(headline,new LinearLayout.LayoutParams(-1,dp(39)));
                    tile.addView(text(s.category+"  ·  "+new SimpleDateFormat("HH:mm",Locale.ENGLISH).format(new Date(s.published)),9,mint,false));
                    tile.setFocusable(true);tile.setOnClickListener(v->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(s.link)));}catch(Exception ignored){}});
                    tile.setOnFocusChangeListener((v,b)->tile.setBackground(shape(b?0xff725A0B:0xff1C2B3A)));
                }
                newsNote=all.size()+" recent headlines";
            }
            updateSummary();
        });
    }
    private android.graphics.drawable.Drawable newsPlaceholder(){
        android.graphics.drawable.GradientDrawable fallback=new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
            new int[]{0xff122033,0xff29476a});
        fallback.setCornerRadius(dp(9));return fallback;
    }
    private void loadThumbnail(String imageUrl, ImageView image){new Thread(()->{try{
        HttpURLConnection c=(HttpURLConnection)new URL(imageUrl).openConnection();c.setConnectTimeout(4500);c.setReadTimeout(7500);c.setRequestProperty("User-Agent","Mozilla/5.0 (JL02TV; Android TV)");c.connect();String mime=c.getContentType();if(mime==null||!mime.startsWith("image/"))return;
        BitmapFactory.Options opts=new BitmapFactory.Options();opts.inSampleSize=1;Bitmap b;try(InputStream in=c.getInputStream()){b=BitmapFactory.decodeStream(in,null,opts);}if(b!=null)ui.post(()->image.setImageBitmap(b));
    }catch(Exception ignored){}}).start();}

    private void loadOpenGraphImage(String page,ImageView target){new Thread(()->{try{
        if(page==null||!page.startsWith("https://"))return;
        HttpURLConnection c=(HttpURLConnection)new URL(page).openConnection();c.setConnectTimeout(3600);c.setReadTimeout(3600);c.setInstanceFollowRedirects(true);c.setRequestProperty("User-Agent","Mozilla/5.0 JL02TV");
        String type=c.getContentType();if(type==null||!type.toLowerCase(Locale.US).contains("html"))return;
        byte[] bytes=new byte[120000];int n;try(InputStream in=c.getInputStream()){n=in.read(bytes);}if(n<=0)return;
        String html=new String(bytes,0,n,"UTF-8");java.util.regex.Matcher m=java.util.regex.Pattern.compile("<meta[^>]+(?:property|name)=[\"'](?:og:image|twitter:image)[\"'][^>]+>",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(html);
        if(m.find()){java.util.regex.Matcher u=java.util.regex.Pattern.compile("content=[\"']([^\"']+)",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(m.group());if(u.find()){String img=u.group(1).replace("&amp;","&");if(img.startsWith("https://"))loadThumbnail(img,target);}}
    }catch(Exception ignored) {}}).start();}

    private Set<String> favorites(){return new HashSet<>(favoritePrefs.getStringSet("favorites",Collections.emptySet()));}
    private void flipFavorite(String pkg){Set<String> f=favorites();if(!f.add(pkg))f.remove(pkg);favoritePrefs.edit().putStringSet("favorites",f).apply();renderApps();}
    private void renderApps(){if(favoriteRow==null||appRow==null)return;
        installedApps.clear();PackageManager pm=getPackageManager();List<ResolveInfo> found=new ArrayList<>();Intent tv=new Intent(Intent.ACTION_MAIN);tv.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);found.addAll(pm.queryIntentActivities(tv,0));Intent other=new Intent(Intent.ACTION_MAIN);other.addCategory(Intent.CATEGORY_LAUNCHER);found.addAll(pm.queryIntentActivities(other,0));Set<String> seen=new HashSet<>();
        for(ResolveInfo ri:found){if(ri.activityInfo==null)continue;String pkg=ri.activityInfo.packageName;if(pkg.equals(getPackageName())||!seen.add(pkg))continue;TVApp a=new TVApp();a.pkg=pkg;a.title=ri.loadLabel(pm).toString();a.icon=ri.loadIcon(pm); try {Drawable better=pm.getApplicationIcon(pkg);if(better!=null)a.icon=better;}catch(Exception ignored){}try {a.banner=pm.getApplicationBanner(pkg);}catch(Exception ignored){}a.intent=pm.getLeanbackLaunchIntentForPackage(pkg);if(a.intent==null)a.intent=pm.getLaunchIntentForPackage(pkg);if(a.intent!=null)installedApps.add(a);}
        Collections.sort(installedApps,(a,b)->a.title.compareToIgnoreCase(b.title));Set<String> f=favorites();List<TVApp> pinned=new ArrayList<>();for(TVApp a:installedApps)if(f.contains(a.pkg))pinned.add(a);
        if(pinned.isEmpty()){String[] defaults={"youtube","netflix","primevideo","disney","spotify","vlc","kodi"};for(String search:defaults){for(TVApp a:installedApps)if(a.pkg.toLowerCase(Locale.US).contains(search)&&!pinned.contains(a)){pinned.add(a);break;}if(pinned.size()>=6)break;}}
        favoriteRow.removeAllViews();appRow.removeAllViews();for(int i=0;i<Math.min(6,pinned.size());i++){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(118),1);lp.rightMargin=dp(9);favoriteRow.addView(appTile(pinned.get(i),true),lp);}
        if(pinned.isEmpty())favoriteRow.addView(action("Choose an app below and hold OK to add it",()->startActivity(new Intent(this,AppsActivity.class))));
        int shown=0;for(TVApp a:installedApps){if(shown>=7)break;if(f.contains(a.pkg))continue;LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(69),1);lp.rightMargin=dp(8);appRow.addView(appTile(a,false),lp);shown++;}appRow.addView(action("+ All Apps",()->startActivity(new Intent(this,AppsActivity.class))));
    }
    private FrameLayout appTile(TVApp a,boolean favorite){
        FrameLayout tile=new FrameLayout(this);tile.setBackground(shape(0xff10151d));tile.setFocusable(true);tile.setClickable(true);
        ImageView artwork=new ImageView(this);
        artwork.setImageDrawable(favorite && a.banner!=null?a.banner:a.icon);
        artwork.setScaleType(favorite?ImageView.ScaleType.FIT_CENTER:ImageView.ScaleType.FIT_CENTER);
        FrameLayout.LayoutParams art=new FrameLayout.LayoutParams(-1,-1);
        art.setMargins(dp(favorite?3:7),dp(favorite?2:4),dp(favorite?3:7),dp(favorite?17:16));
        tile.addView(artwork,art);
        TextView title=text(a.title,favorite?12:10,white,true);title.setGravity(Gravity.CENTER);
        title.setSingleLine(true);title.setEllipsize(TextUtils.TruncateAt.END);
        title.setBackgroundColor(0xAD070B12);
        FrameLayout.LayoutParams label=new FrameLayout.LayoutParams(-1,dp(favorite?23:18),Gravity.BOTTOM);
        tile.addView(title,label);
        tile.setOnClickListener(v->{try{startActivity(a.intent);}catch(Exception ignored){}});
        tile.setOnLongClickListener(v->{flipFavorite(a.pkg);return true;});
        tile.setOnKeyListener((v,key,e)->{if(e.getAction()==KeyEvent.ACTION_UP&&(key==KeyEvent.KEYCODE_MENU||key==KeyEvent.KEYCODE_INFO)){flipFavorite(a.pkg);return true;}return false;});
        tile.setOnFocusChangeListener((v,b)->{tile.setBackground(shape(b?0xff8B6900:0xff10151d));tile.setScaleX(b?1.035f:1f);tile.setScaleY(b?1.035f:1f);});
        return tile;
    }

    private void fetchWeather(){try{
        // KL default coordinates; no location permissions or device tracking necessary.
        String url="https://api.open-meteo.com/v1/forecast?latitude=3.1390&longitude=101.6869&timezone=Asia%2FKuala_Lumpur&current=temperature_2m,relative_humidity_2m,weather_code&hourly=temperature_2m,precipitation_probability,weather_code&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max,weather_code&forecast_days=4";
        JSONObject root=new JSONObject(read(open(url)));JSONObject now=root.getJSONObject("current"),hours=root.getJSONObject("hourly"),days=root.getJSONObject("daily");
        int temperature=(int)Math.round(now.getDouble("temperature_2m"));int humid=now.getInt("relative_humidity_2m");
        JSONArray htime=hours.getJSONArray("time"),htemp=hours.getJSONArray("temperature_2m"),hrain=hours.getJSONArray("precipitation_probability");
        JSONArray dtime=days.getJSONArray("time"),dhi=days.getJSONArray("temperature_2m_max"),dlo=days.getJSONArray("temperature_2m_min"),drain=days.getJSONArray("precipitation_probability_max");
        String today=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());List<String[]> hourList=new ArrayList<>(),dayList=new ArrayList<>();int max=0;
        for(int i=0;i<htime.length();i++){String stamp=htime.getString(i);if(stamp.compareTo(new SimpleDateFormat("yyyy-MM-dd'T'HH",Locale.US).format(new Date()))>=0 && hourList.size()<8){int rain=hrain.optInt(i,0);max=Math.max(max,rain);hourList.add(new String[]{stamp.substring(11),String.valueOf((int)Math.round(htemp.getDouble(i)))+"°",rain+"%"});}}
        for(int i=1;i<Math.min(4,dtime.length());i++)dayList.add(new String[]{dtime.getString(i),Math.round(dhi.getDouble(i))+"° / "+Math.round(dlo.getDouble(i))+"°",drain.optInt(i)+"% rain"});
        final int rain=max;ui.post(()->{highRain=rain;weatherNow.setText(temperature+"°C  ·  "+humid+"% humidity");weatherStatus.setText("Today · hourly precipitation outlook");hourlyRows.removeAllViews();for(String[] h:hourList){LinearLayout col=vertical();col.setPadding(dp(11),dp(8),dp(11),dp(8));col.addView(text(h[0],11,muted,true));col.addView(text(h[1],18,white,true));col.addView(text("☂ "+h[2],11,mint,false));hourlyRows.addView(col);}threeDays.removeAllViews();for(String[] d:dayList){LinearLayout day=vertical();day.setBackground(shape(0xff26364a));day.setPadding(dp(12),dp(10),dp(8),dp(8));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(92),1);lp.rightMargin=dp(8);threeDays.addView(day,lp);day.addView(text(d[0].substring(5),13,mint,true));day.addView(text(d[1],16,white,true));day.addView(text("☂ "+d[2],12,muted,false));}todayForecast="Currently "+temperature+" degrees Celsius with "+humid+" percent humidity. Today peak hourly rain chance is "+rain+" percent.";StringBuilder next=new StringBuilder();for(String[] d:dayList)next.append(d[0]).append(": ").append(d[1].replace("°", " degrees ")).append(", ").append(d[2]).append(". ");nextForecast=next.toString();weatherNote=rain>=60?"Possible heavy rain today (up to "+rain+"% hourly probability)":"Today's maximum hourly rain chance: "+rain+"%";updateSummary();});
        }catch(Exception e){ui.post(()->{weatherNow.setText("Forecast unavailable");weatherStatus.setText("Connect to internet, then select Refresh");weatherNote="Weather not updated";todayForecast="Forecast unavailable";nextForecast="Three-day forecast unavailable";updateSummary();});}}
    private void connectMicrosoft(){
        if(!microsoft.isConfigured()){message("This APK needs a Microsoft application ID set once at GitHub build time. See MICROSOFT-SETUP.md. No company password or server is needed on the TV.");return;}
        microsoft.signIn(()->loadCalendar());
    }
    private void showCalendarOptions(){
        String[] opts={"Connect Microsoft 365 on my phone","Refresh work schedule","Disconnect Microsoft 365","Use calendar on this TV","Add Google Calendar iCal link (read-only)","Send Google iCal link from phone (QR)","Remove Google Calendar link","Speak meeting titles: "+(speakMeetingTitles?"ON":"OFF")};
        new AlertDialog.Builder(this).setTitle("JL02 TV · Calendar").setItems(opts,(d,i)->{
            switch(i){case 0:connectMicrosoft();break;case 1:loadCalendar();break;case 2:microsoft.disconnect();meetings.clear();loadCalendar();break;case 3:requestCalendar();break;case 4:showGoogleLinkInput();break;case 5:showPhoneTransfer();break;case 6:favoritePrefs.edit().remove("google_ical").apply();loadCalendar();break;case 7:speakMeetingTitles=!speakMeetingTitles;break;}
        }).setNegativeButton("Close",null).show();
    }

    private LocalIcalTransfer activeTransfer;
    private void showPhoneTransfer(){
        new AlertDialog.Builder(this).setTitle("Send your Google Calendar link from your phone")
            .setMessage("Use only a trusted private home Wi-Fi network. Your phone and TV must be on the same Wi-Fi. This temporary local connection is HTTP, not end-to-end encrypted: other devices on the network could potentially intercept the secret calendar link. For stronger privacy, use manual entry instead. Continue?")
            .setPositiveButton("Use trusted home Wi-Fi",(d,w)->startPhoneTransfer())
            .setNegativeButton("Cancel",null).show();
    }
    private void startPhoneTransfer(){
        try{
            if(activeTransfer!=null)activeTransfer.close();
            activeTransfer=new LocalIcalTransfer(this,link->{
                new AlertDialog.Builder(this).setTitle("Accept personal calendar link?")
                    .setMessage("A phone submitted a Google Calendar iCal link. Save it on this TV? Confirm only if you sent it. The link grants read access to your calendar.")
                    .setPositiveButton("Save & sync",(dialog,which)->{
                        favoritePrefs.edit().putString("google_ical",link).apply();
                        if(activeTransfer!=null){activeTransfer.close();activeTransfer=null;}
                        loadCalendar();
                    }).setNegativeButton("Reject",(dialog,which)->{
                        if(activeTransfer!=null){activeTransfer.close();activeTransfer=null;}
                    }).show();
            });
            String address=activeTransfer.start();
            BitMatrix bits=new MultiFormatWriter().encode(address,BarcodeFormat.QR_CODE,310,310);
            Bitmap bmp=Bitmap.createBitmap(310,310,Bitmap.Config.ARGB_8888);
            for(int y=0;y<310;y++)for(int x=0;x<310;x++)bmp.setPixel(x,y,bits.get(x,y)?Color.BLACK:Color.WHITE);
            ImageView img=new ImageView(this);img.setImageBitmap(bmp);
            img.setPadding(dp(12),dp(12),dp(12),dp(12));
            new AlertDialog.Builder(this).setTitle("Scan on your phone · expires in 3 minutes")
                .setMessage("Your phone must be on the same trusted Wi-Fi. Paste the Secret iCal link into the phone page. Confirm the transfer on TV. Local HTTP is not encrypted.\n\n"+address)
                .setView(img).setNegativeButton("Stop",(d,w)->{if(activeTransfer!=null){activeTransfer.close();activeTransfer=null;}})
                .setOnDismissListener(d->{ /* Keep session until expiry or transfer confirmation */ }).show();
        }catch(Exception e){message("Could not start local pairing. Check your Wi-Fi connection: "+e.getMessage());}
    }

    private void showGoogleLinkInput(){
        EditText input=new EditText(this);input.setSingleLine();input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);
        input.setHint("https://calendar.google.com/calendar/ical/…/basic.ics");
        new AlertDialog.Builder(this).setTitle("Google Calendar · read-only iCal")
            .setMessage("On a computer, open Google Calendar → Settings → your personal calendar → Integrate calendar → Secret address in iCal format. Copy the HTTPS link. Treat it like a password: anyone holding it may read that calendar. Use only your personal calendar, never a restricted company calendar. The link is stored locally on this TV.")
            .setView(input).setPositiveButton("Save & sync",(dialog,which)->{String url=input.getText().toString().trim();if(!url.startsWith("https://")||!url.endsWith(".ics")){message("Paste the full HTTPS Secret address in iCal format ending in .ics.");return;} favoritePrefs.edit().putString("google_ical",url).apply();loadCalendar();})
            .setNegativeButton("Cancel",null).show();
    }
    private void loadGoogleIcal(){String url=favoritePrefs.getString("google_ical","");if(url.isEmpty())return;
        calendarStatus.setText("Updating personal Google Calendar…");
        new Thread(()->{try{String ics=read(open(url));java.util.ArrayList<String> entries=new java.util.ArrayList<>();String today=new SimpleDateFormat("yyyyMMdd",Locale.US).format(new Date());String[] blocks=ics.replace("\r","").replace("\n ","").split("BEGIN:VEVENT");
            for(int i=1;i<blocks.length&&entries.size()<12;i++){String block=blocks[i];String title="Busy",start="";for(String line:block.split("\n")){if(line.startsWith("SUMMARY:"))title=line.substring(8).replace("\\,",",");if(line.startsWith("DTSTART")&&line.contains(":"))start=line.substring(line.indexOf(':')+1).trim();}if(start.length()>=8 && start.startsWith(today)){String hour=start.length()>=13?start.substring(9,11)+":"+start.substring(11,13):"All day";entries.add(hour+" · "+title);}}
            ui.post(()->{meetings.clear();meetings.addAll(entries);scheduleRows.removeAllViews();calendarStatus.setText(entries.size()+" Google event(s) today");if(entries.isEmpty())line(scheduleRows,"No Google events found today",12,muted);for(String e:entries)line(scheduleRows,e,12,white);updateSummary();});
        }catch(Exception ex){ui.post(()->{calendarStatus.setText("Google iCal could not be loaded");scheduleRows.removeAllViews();line(scheduleRows,"Check the iCal link or network",12,muted);});}}).start();
    }
    private void configureServer(){EditText input=new EditText(this);input.setSingleLine();input.setText(syncUrl);input.setHint("https://your-secure-service.example");
        new AlertDialog.Builder(this).setTitle("Secure calendar service HTTPS URL").setView(input).setPositiveButton("Save",(d,w)->{
            String u=input.getText().toString().trim().replaceAll("/+$","");
            if(!u.startsWith("https://")){message("HTTPS is required");return;}
            if(!u.equals(syncUrl)){syncUrl=u;deviceToken="";storeConnection();}loadCalendar();
        }).setNegativeButton("Cancel",null).show();}
    private void testServerConnection(){
        if(syncUrl.isEmpty()){message("Set your deployed HTTPS calendar server URL first. The APK does not host the calendar server.");return;}
        new Thread(()->{try{
            JSONObject health=new JSONObject(read(open(syncUrl+"/health")));
            boolean google=health.optBoolean("google_configured"),microsoft=health.optBoolean("microsoft_configured");
            String result="Server reachable: "+health.optString("status","unknown")+"\nGoogle OAuth: "+(google?"configured":"NOT configured")+"\nMicrosoft OAuth: "+(microsoft?"configured":"NOT configured")+"\n\nIf a provider isn't configured, add its OAuth client credentials to the server, not the TV.";
            ui.post(()->message(result));
        }catch(Exception e){ui.post(()->message("Calendar server not reachable. Verify the HTTPS URL, certificate, internet access, and deployed backend.\n\n"+e.getMessage()));}}).start();
    }
    private void storeConnection(){getPreferences(0).edit().putString("device_token",deviceToken).apply();}
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
    private void beginPairing(){if(syncUrl.isEmpty()){message("Google / Outlook pairing is not active in this build. No link entry is required. You can use calendars already on the TV from Calendar options.");return;}
        new Thread(()->{try{ensureDevice();String link=api("POST","/v1/pair",new JSONObject()).getString("url");ui.post(()->showPairQR(link));}
        catch(Exception e){ui.post(()->message("Could not begin pairing: "+e.getMessage()));}}).start();}
    private void showPairQR(String link){try{
        BitMatrix bits=new MultiFormatWriter().encode(link,BarcodeFormat.QR_CODE,360,360);
        Bitmap bitmap=Bitmap.createBitmap(360,360,Bitmap.Config.ARGB_8888);
        for(int y=0;y<360;y++)for(int x=0;x<360;x++)bitmap.setPixel(x,y,bits.get(x,y)?android.graphics.Color.BLACK:android.graphics.Color.WHITE);
        ImageView image=new ImageView(this);image.setImageBitmap(bitmap);image.setPadding(dp(30),dp(20),dp(30),dp(20));
        new AlertDialog.Builder(this).setTitle("Scan with your phone · expires in 10 minutes").setView(image)
            .setMessage("Scan the QR with your phone. If scanning fails, use this link shown below:\n"+link+"\n\nThe link must use your deployed HTTPS server. Then select Refresh schedule on the TV.")
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
    private void loadCalendar(){
        if(favoritePrefs!=null&&!favoritePrefs.getString("google_ical","").isEmpty() && (microsoft==null||!microsoft.isConnected())){loadGoogleIcal();return;}
        if(microsoft!=null && microsoft.isConnected()){
            calendarStatus.setText("Updating Microsoft 365 calendar…");
            microsoft.loadToday(result->{
                scheduleRows.removeAllViews();meetings.clear();
                if(result.error!=null){calendarStatus.setText("Microsoft calendar unavailable");line(scheduleRows,result.error,11,muted);}
                else {meetings.addAll(result.events);calendarStatus.setText(result.events.size()+" work event(s) today");
                    if(result.events.isEmpty())line(scheduleRows,"No meetings scheduled today",12,muted);
                    for(String event:result.events)line(scheduleRows,event,12,white);}
                updateSummary();
            });return;
        }
        if(microsoft!=null&&microsoft.isConfigured()){
            calendarStatus.setText("Microsoft 365 not connected");scheduleRows.removeAllViews();line(scheduleRows,"Choose Microsoft 365 sign-in",11,muted);return;
        }
        if(!syncUrl.isEmpty() && !syncUrl.startsWith("https://")){calendarStatus.setText("Secure calendar service configuration is invalid");return;} if(!syncUrl.isEmpty()){
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
        if(!BuildConfig.VOICE_PROXY_URL.isEmpty()){playNaturalBriefing();return;}
        if(!speechReady || speech==null){voiceButton.setText("Install/enable Android Text-to-Speech");return;}
        String narration=createNarration();
        speaking=true;voiceButton.setText("■  Stop briefing");
        // QUEUE_FLUSH prevents multiple overlapping dashboard readings.
        int result=speech.speak(narration,TextToSpeech.QUEUE_FLUSH,null,"jl02_tv_daily_briefing");
        if(result==TextToSpeech.ERROR){speaking=false;voiceButton.setText("▶  Voice unavailable");}
    }
    private synchronized void releaseNatural(){try{if(naturalPlayer!=null){naturalPlayer.stop();naturalPlayer.release();naturalPlayer=null;}}catch(Exception ignored){} if(audioCache!=null){audioCache.delete();audioCache=null;}}
    private void playNaturalBriefing(){
        speaking=true;voiceButton.setText("◌  Preparing natural voice…");String narration=createNarration();
        new Thread(()->{java.io.File cache=null;try{
            URL url=new URL(BuildConfig.VOICE_PROXY_URL);if(!"https".equalsIgnoreCase(url.getProtocol()))throw new Exception("HTTPS required");
            HttpURLConnection conn=(HttpURLConnection)url.openConnection();conn.setRequestMethod("POST");conn.setDoOutput(true);conn.setConnectTimeout(8000);conn.setReadTimeout(25000);conn.setRequestProperty("Content-Type","application/json");
            byte[] payload=new JSONObject().put("text",narration).toString().getBytes("UTF-8");conn.getOutputStream().write(payload);
            if(conn.getResponseCode()!=200)throw new Exception("Voice service HTTP "+conn.getResponseCode());
            cache=java.io.File.createTempFile("jl02-voice-",".mp3",getCacheDir());try(InputStream in=conn.getInputStream();java.io.FileOutputStream f=new java.io.FileOutputStream(cache)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)f.write(b,0,n);}
            java.io.File file=cache;ui.post(()->{if(!speaking){file.delete();return;}try{audioCache=file;naturalPlayer=new MediaPlayer();naturalPlayer.setDataSource(file.getAbsolutePath());naturalPlayer.setOnCompletionListener(mp->{releaseNatural();speaking=false;voiceButton.setText("▶  Listen to briefing");});naturalPlayer.prepare();naturalPlayer.start();voiceButton.setText("■  Stop briefing");}catch(Exception e){releaseNatural();speaking=false;voiceButton.setText("▶  Voice unavailable");}});
        }catch(Exception e){if(cache!=null)cache.delete();ui.post(()->{speaking=false;voiceButton.setText("▶  Voice offline · use local TTS");});}}
        ).start();
    }
    private void stopSpeaking(){
        releaseNatural();if(speech!=null)speech.stop();speaking=false;
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
