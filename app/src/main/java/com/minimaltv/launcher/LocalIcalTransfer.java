package com.minimaltv.launcher;

import android.app.Activity;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;

/** Short-lived local Wi-Fi transfer. HTTP has no confidentiality: trusted networks only. */
public final class LocalIcalTransfer {
    public interface Receiver { void onReceive(String url); }
    private final Activity activity;
    private final Receiver receiver;
    private final String token;
    private volatile boolean closed=false;
    private ServerSocket server;
    private final long expires=System.currentTimeMillis()+180000;
    public LocalIcalTransfer(Activity a,Receiver r){ activity=a;receiver=r;
        byte[] b=new byte[24];new SecureRandom().nextBytes(b);
        token=android.util.Base64.encodeToString(b,android.util.Base64.URL_SAFE|android.util.Base64.NO_WRAP|android.util.Base64.NO_PADDING);
    }
    public String start() throws Exception {
        String ip=null;
        Enumeration<NetworkInterface> nets=NetworkInterface.getNetworkInterfaces();
        while(nets.hasMoreElements()){
            NetworkInterface ni=nets.nextElement();if(!ni.isUp()||ni.isLoopback())continue;
            Enumeration<InetAddress> addrs=ni.getInetAddresses();
            while(addrs.hasMoreElements()){
                InetAddress a=addrs.nextElement();if(a instanceof Inet4Address && a.isSiteLocalAddress()){
                    if(ni.getName().startsWith("wlan")||ni.getName().startsWith("eth")){ip=a.getHostAddress();break;}
                    if(ip==null)ip=a.getHostAddress();
                }
            }
            if(ip!=null && (ni.getName().startsWith("wlan")||ni.getName().startsWith("eth")))break;
        }
        if(ip==null)throw new IOException("No local Wi-Fi/LAN IP found");
        server=new ServerSocket(0,4);server.setSoTimeout(1000);
        String link="http://"+ip+":"+server.getLocalPort()+"/pair/"+token;
        new Thread(()->serve(),"jl02-local-ical").start();return link;
    }
    private void serve(){
        while(!closed && System.currentTimeMillis()<expires){
            try(Socket socket=server.accept()){
                socket.setSoTimeout(5000);
                BufferedInputStream in=new BufferedInputStream(socket.getInputStream());
                ByteArrayOutputStream head=new ByteArrayOutputStream();int prev=-1,c;
                while(head.size()<10000 && (c=in.read())!=-1){head.write(c);
                    byte[] b=head.toByteArray();int n=b.length;
                    if(n>=4&&b[n-4]=='\r'&&b[n-3]=='\n'&&b[n-2]=='\r'&&b[n-1]=='\n')break;
                }
                String req=new String(head.toByteArray(),StandardCharsets.UTF_8);
                String[] rows=req.split("\\r?\\n");if(rows.length==0)continue;
                String[] line=rows[0].split(" ");if(line.length<2)continue;
                String path="/pair/"+token;
                if(!line[1].equals(path)){reply(socket,404,"Not found");continue;}
                if(line[0].equals("GET")){
                    reply(socket,200,"<!doctype html><meta name='viewport' content='width=device-width, initial-scale=1'><title>JL02 Calendar Transfer</title><style>body{font:18px system-ui;max-width:550px;margin:32px auto;padding:20px;background:#151515;color:white}input,button{box-sizing:border-box;font:16px system-ui;width:100%;padding:14px;margin-top:12px}button{background:#f4d300;border:0}</style><h2>JL02 TV · Google Calendar</h2><p>Paste your personal <b>Secret address in iCal format</b>. This connection works only on your private home Wi-Fi and is not encrypted.</p><form method='POST'><input type='url' name='ical' required placeholder='https://calendar.google.com/calendar/ical/.../basic.ics'><button>Send to TV</button></form><p>Approve the transfer on your TV. This page expires in 3 minutes.</p>");
                } else if(line[0].equals("POST")){
                    int length=0;for(String row:rows){if(row.toLowerCase(Locale.US).startsWith("content-length:"))try{length=Integer.parseInt(row.split(":",2)[1].trim());}catch(Exception ignored){}}
                    if(length<=0||length>8192){reply(socket,400,"Invalid request");continue;}
                    byte[] body=new byte[length];int n=0;while(n<length){int count=in.read(body,n,length-n);if(count<0)break;n+=count;}
                    String form=new String(body,0,n,StandardCharsets.UTF_8);
                    String url="";
                    for(String piece:form.split("&")){if(piece.startsWith("ical="))url=URLDecoder.decode(piece.substring(5),"UTF-8");}
                    if(!url.startsWith("https://calendar.google.com/calendar/ical/")||!url.contains(".ics")||url.length()>4096){reply(socket,400,"Invalid Google iCal URL");continue;}
                    final String delivered=url;
                    reply(socket,200,"<!doctype html><meta name='viewport' content='width=device-width, initial-scale=1'><body style='font:20px system-ui;padding:30px'>Link sent. <b>Approve it on your TV.</b></body>");
                    activity.runOnUiThread(()->receiver.onReceive(delivered));
                    close();
                }else reply(socket,405,"Method not allowed");
            }catch(SocketTimeoutException ignored){}catch(Exception ignored){}
        }
        close();
    }
    private void reply(Socket socket,int code,String message)throws IOException{
        byte[] body=message.getBytes(StandardCharsets.UTF_8);
        OutputStream out=socket.getOutputStream();
        out.write(("HTTP/1.1 "+code+" OK\r\nContent-Type: text/html; charset=utf-8\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\nContent-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; form-action 'self'\r\nContent-Length: "+body.length+"\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));out.write(body);out.flush();
    }
    public void close(){closed=true;try{if(server!=null)server.close();}catch(Exception ignored){}}
}
