package com.minimaltv.launcher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Remote-first TV launcher; no networking, services, analytics or ad SDKs. */
public class AppsActivity extends Activity {
    private final int bg=Color.rgb(11,16,30), tile=Color.rgb(26,35,54), accent=Color.rgb(111,229,203);
    private LinearLayout page, favoritesRow, appsGrid;
    private TextView clock, date, headline;
    private SharedPreferences prefs;
    private final Handler handler=new Handler();
    private final List<AppInfo> apps=new ArrayList<>();
    private boolean showAll=false;
    private static class AppInfo { String pkg, title; Drawable icon; Intent intent; }
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    @Override public void onCreate(Bundle saved){super.onCreate(saved);getWindow().getDecorView().setSystemUiVisibility(5894|1024|512);prefs=getSharedPreferences("home",MODE_PRIVATE);readApps();build();handler.post(tick);}
    @Override protected void onResume(){super.onResume();readApps();if(page!=null)populate();}
    @Override protected void onDestroy(){handler.removeCallbacks(tick);super.onDestroy();}
    private final Runnable tick=new Runnable(){public void run(){if(clock!=null){clock.setText(new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date()));date.setText(new SimpleDateFormat("EEEE, d MMMM",Locale.getDefault()).format(new Date()));}handler.postDelayed(this,30000);}};
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView label(String s,int sp,int color,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextColor(color);v.setTextSize(sp);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout horizontal(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private void readApps(){
        apps.clear();PackageManager pm=getPackageManager();List<ResolveInfo> all=new ArrayList<>();
        Intent tv=new Intent(Intent.ACTION_MAIN);tv.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);all.addAll(pm.queryIntentActivities(tv,0));
        Intent normal=new Intent(Intent.ACTION_MAIN);normal.addCategory(Intent.CATEGORY_LAUNCHER);all.addAll(pm.queryIntentActivities(normal,0));
        Set<String> seen=new HashSet<>();
        for(ResolveInfo r:all){if(r.activityInfo==null)continue;String pkg=r.activityInfo.packageName;if(pkg.equals(getPackageName())||!seen.add(pkg))continue;
            AppInfo app=new AppInfo();app.pkg=pkg;app.title=r.loadLabel(pm).toString();app.icon=r.loadIcon(pm);
            Intent launch=pm.getLeanbackLaunchIntentForPackage(pkg);if(launch==null)launch=pm.getLaunchIntentForPackage(pkg);
            if(launch==null){launch=new Intent(Intent.ACTION_MAIN);launch.setClassName(pkg,r.activityInfo.name);launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);}
            app.intent=launch;apps.add(app);
        }
        Collections.sort(apps,Comparator.comparing(a->a.title.toLowerCase(Locale.getDefault())));
    }
    private void build(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(15,26,44),bg,Color.rgb(9,13,24)}));sc.setVerticalScrollBarEnabled(false);
        page=vertical();page.setPadding(dp(62),dp(38),dp(62),dp(35));sc.addView(page);setContentView(sc);
        LinearLayout top=horizontal();page.addView(top,new LinearLayout.LayoutParams(-1,dp(60)));
        TextView brand=label("◈  JL02 TV",19,accent,true);top.addView(brand,new LinearLayout.LayoutParams(0,-1,1));
        LinearLayout timeGroup=vertical();timeGroup.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);top.addView(timeGroup);
        clock=label("",28,Color.WHITE,true);clock.setGravity(Gravity.RIGHT);timeGroup.addView(clock);
        date=label("",12,0xffA7B7CB,false);date.setGravity(Gravity.RIGHT);timeGroup.addView(date);
        TextView spacer=label("",8,Color.WHITE,false);page.addView(spacer,new LinearLayout.LayoutParams(-1,dp(48)));
        headline=label("Good to see you.",37,Color.WHITE,true);page.addView(headline);
        TextView hint=label("Everything you need. Nothing you don't.",17,0xff98AABE,false);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,dp(45));page.addView(hint,hp);
        section("FAVORITES",18);favoritesRow=horizontal();favoritesRow.setGravity(Gravity.LEFT|Gravity.TOP);page.addView(favoritesRow);
        LinearLayout actions=horizontal();LinearLayout.LayoutParams acp=new LinearLayout.LayoutParams(-1,dp(95));acp.topMargin=dp(20);page.addView(actions,acp);
        addAction(actions,"▦  All Apps",()->{showAll=!showAll;populate();});
        addAction(actions,"⚙  Settings",()->openSettings(Settings.ACTION_SETTINGS));
        addAction(actions,"◉  Wi-Fi",()->openSettings(Settings.ACTION_WIFI_SETTINGS));
        section("ALL APPS",17);appsGrid=vertical();page.addView(appsGrid);
        TextView foot=label("OK to open  •  Hold OK to add or remove favorite  •  BACK to close apps list",13,0xff7890A9,false);LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,dp(44));fp.topMargin=dp(20);page.addView(foot,fp);
        populate();
    }
    private void section(String s,int size){TextView label=label(s,size,0xffC8D6E5,true);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(46));p.topMargin=dp(12);page.addView(label,p);}
    private void addAction(LinearLayout parent,String title,Runnable run){TextView b=label(title,17,Color.WHITE,true);b.setGravity(Gravity.CENTER);b.setBackground(shape(tile,13));b.setFocusable(true);b.setOnClickListener(v->run.run());b.setOnFocusChangeListener((v,focus)->v.setBackground(shape(focus?0xff176D67:tile,13)));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(205),dp(62));lp.rightMargin=dp(16);parent.addView(b,lp);}
    private Set<String> favorites(){return new HashSet<>(prefs.getStringSet("favorites",Collections.emptySet()));}
    private void changeFavorite(String pkg){Set<String> f=favorites();if(!f.add(pkg))f.remove(pkg);prefs.edit().putStringSet("favorites",f).apply();populate();}
    private void launch(AppInfo app){try{startActivity(app.intent);}catch(Exception e){android.widget.Toast.makeText(this,"Could not open "+app.title,android.widget.Toast.LENGTH_SHORT).show();}}
    private LinearLayout appCard(AppInfo app,boolean isFavorite){
        LinearLayout card=vertical();card.setGravity(Gravity.CENTER);card.setFocusable(true);card.setClickable(true);card.setBackground(shape(tile,18));card.setPadding(dp(12),dp(13),dp(12),dp(12));
        ImageView icon=new ImageView(this);icon.setImageDrawable(app.icon);icon.setScaleType(ImageView.ScaleType.FIT_CENTER);card.addView(icon,new LinearLayout.LayoutParams(dp(55),dp(55)));
        TextView title=label(app.title,15,Color.WHITE,true);title.setGravity(Gravity.CENTER);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,dp(42));tp.topMargin=dp(8);card.addView(title,tp);
        if(isFavorite){TextView star=label("★",13,accent,true);star.setGravity(Gravity.CENTER);card.addView(star);}
        card.setOnClickListener(v->launch(app));card.setOnLongClickListener(v->{changeFavorite(app.pkg);return true;});
        card.setOnKeyListener((v,key,event)->{if((key==KeyEvent.KEYCODE_MENU || key==KeyEvent.KEYCODE_INFO) && event.getAction()==KeyEvent.ACTION_UP){changeFavorite(app.pkg);return true;}return false;});
        card.setOnFocusChangeListener((v,focused)->{card.setBackground(shape(focused?0xff206B6A:tile,18));card.setScaleX(focused?1.05f:1f);card.setScaleY(focused?1.05f:1f);});return card;
    }
    private void populate(){if(favoritesRow==null)return;favoritesRow.removeAllViews();appsGrid.removeAllViews();Set<String> f=favorites();
        List<AppInfo> pinned=new ArrayList<>();for(AppInfo a:apps)if(f.contains(a.pkg))pinned.add(a);
        if(pinned.isEmpty()){String[] suggest={"youtube","netflix","primevideo","disney","spotify","vlc","kodi"};for(String target:suggest){for(AppInfo a:apps){if(a.pkg.toLowerCase().contains(target)&&!pinned.contains(a)){pinned.add(a);break;}}if(pinned.size()>=5)break;}if(pinned.isEmpty())pinned.addAll(apps.subList(0,Math.min(5,apps.size())));}
        for(int i=0;i<Math.min(6,pinned.size());i++){AppInfo a=pinned.get(i);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(154),1);p.rightMargin=dp(13);favoritesRow.addView(appCard(a,true),p);}
        if(pinned.size()<6){TextView add=label("+\n\nAdd favorites",16,0xffB8D2DB,true);add.setGravity(Gravity.CENTER);add.setBackground(shape(0xff15263A,18));add.setFocusable(true);add.setOnClickListener(v->{showAll=true;populate();});add.setOnFocusChangeListener((v,b)->v.setBackground(shape(b?0xff206B6A:0xff15263A,18)));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(154),1);favoritesRow.addView(add,p);}
        if(!showAll){TextView msg=label("Choose All Apps above to browse installed applications.",15,0xff8199AD,false);appsGrid.addView(msg,new LinearLayout.LayoutParams(-1,dp(48)));return;}
        for(int i=0;i<apps.size();i+=6){LinearLayout row=horizontal();LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(148));rp.bottomMargin=dp(14);appsGrid.addView(row,rp);for(int j=i;j<Math.min(i+6,apps.size());j++){AppInfo a=apps.get(j);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);p.rightMargin=dp(13);row.addView(appCard(a,f.contains(a.pkg)),p);}for(int j=Math.min(i+6,apps.size());j<i+6;j++){row.addView(new View(this),new LinearLayout.LayoutParams(0,1,1));}}
    }
    private void openSettings(String action){try{startActivity(new Intent(action));}catch(ActivityNotFoundException e){android.widget.Toast.makeText(this,"Settings unavailable",android.widget.Toast.LENGTH_SHORT).show();}}
    @Override public void onBackPressed(){if(showAll){showAll=false;populate();}else{super.onBackPressed();}}
}
