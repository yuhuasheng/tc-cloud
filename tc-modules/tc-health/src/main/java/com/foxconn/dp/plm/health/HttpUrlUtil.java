package com.foxconn.dp.plm.health;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

public class HttpUrlUtil {

    static {
        // 允许设置所有的头，还没测试
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
    }

    private static Proxy defaultProxy = null;

    public static void setDefaultProxy(Proxy proxy){
        defaultProxy = proxy;
    }

    public static Proxy makeProxy(String ip, int port){
        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ip,port));
    }

    public static String get(String url) {
        return get(url,null,null);
    }



    public static String get(String url, Map<String,String> headerMap, Proxy proxy) {
        StringBuilder result = new StringBuilder();
        BufferedReader in = null;
        try {
            URL httpUrl = new URL(url);
            HttpURLConnection conn;
            if(proxy != null){
                conn = (HttpURLConnection) httpUrl.openConnection(proxy);
            }else if(defaultProxy != null){
                conn = (HttpURLConnection) httpUrl.openConnection(defaultProxy);
            }else {
                conn = (HttpURLConnection) httpUrl.openConnection();
            }
            conn.setConnectTimeout(5000);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            if (headerMap != null) {
                Set<String> keys = headerMap.keySet();
                for (String key : keys) {
                    conn.setRequestProperty(key,headerMap.get(key));
                }
            }
            conn.setRequestMethod("GET");
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("Connection", "close");
            conn.setRequestProperty("Charset", "UTF-8");
            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new Exception(String.format("http request fail: httpCode:%d content:%s", responseCode, conn.getContent()));
            }
            //定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            char[] chars = new char[1024];
            int num;
            while ((num = in.read(chars))!=-1) {
                result.append(chars,0,num);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result.toString();
    }

    public static String postWithFormUrlencoded(String url, Map<String, String> form) {
        return postWithFormUrlencoded(url,form,null,null);
    }

    public static String postWithFormUrlencoded(String url, Map<String, String> form,Map<String,String> header) {
        return postWithFormUrlencoded(url,form,header,null);
    }

    public static String postWithFormUrlencoded(String url,  Map<String, String> form, Map<String,String> header, Proxy proxy) {
        StringBuilder result = new StringBuilder();
        PrintWriter out = null;
        BufferedReader in = null;
        HttpURLConnection conn = null;
        try {
            URL httpUrl = new URL(url);

            if(proxy != null){
                conn = (HttpURLConnection) httpUrl.openConnection(proxy);
            }else if(defaultProxy != null){
                conn = (HttpURLConnection) httpUrl.openConnection(defaultProxy);
            }else {
                conn = (HttpURLConnection) httpUrl.openConnection();
            }
            conn.setConnectTimeout(5000);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("Connection", "close");
            conn.setRequestProperty("Charset", "UTF-8");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");//关键代码 application/x-www-form-urlencoded
            if (header != null) {
                Set<String> keys = header.keySet();
                for (String key : keys) {
                    conn.setRequestProperty(key,header.get(key));
                }
            }
            StringBuilder p = new StringBuilder();
            Set<String> keys = form.keySet();
            for (String key : keys) {
                p.append(key).append("=").append(URLEncoder.encode(form.get(key), "UTF-8")).append("&");
            }
            if (p.toString().endsWith("&")) {
                p = new StringBuilder(p.substring(0, p.length() - 1));
            }
            out = new PrintWriter(conn.getOutputStream());
            out.print(p);
            out.flush();
            int responseCode = conn.getResponseCode();
            if(responseCode == HttpURLConnection.HTTP_MOVED_TEMP){
                return conn.getHeaderField("location");
            }
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new Exception(String.format("http request fail: httpCode:%d content:%s", responseCode, conn.getContent()));
            }

            //定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if(conn!=null){
                conn.disconnect();
            }
            if (out != null) {
                out.close();
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result.toString();
    }

    public static String postWithBodyJson(String url,String json){
        String ret = null;
        try {
            //创建连接
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setUseCaches(false);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Safari/537.36");
            connection.connect();
            // POST请求
            DataOutputStream out = new
                    DataOutputStream(connection.getOutputStream());
            out.write(json.getBytes(StandardCharsets.UTF_8));
            out.flush();
            out.close();
            // 读取响应
            BufferedReader reader = new BufferedReader(new
                    InputStreamReader(connection.getInputStream()));
            String lines;
            StringBuilder sb = new StringBuilder("");
            while ((lines = reader.readLine()) != null) {
                lines = URLDecoder.decode(lines, "utf-8");
                sb.append(lines);
            }
            ret = (sb.toString());
            reader.close();
            // 断开连接
            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

}
