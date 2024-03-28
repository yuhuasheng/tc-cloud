package com.foxconn.plm.utils.net;

import cn.hutool.http.HttpRequest;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSON;
import com.foxconn.plm.utils.string.StringUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.CharsetUtils;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class HttpUtil {
    private static Log log = LogFactory.get();

    public static String post(String actionUrl, String params) throws IOException {
        String results = null;
        InputStream is = null;
        OutputStreamWriter writer = null;
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            String serverURL = actionUrl;
            StringBuffer sbf = new StringBuffer();
            String strRead = null;
            URL url = new URL(serverURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");// 璇锋眰post鏂瑰紡
            connection.setDoInput(true);
            connection.setDoOutput(true);
            // header鍐呯殑鐨勫弬鏁板湪杩欓噷set
            // connection.setRequestProperty("key", "value");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.connect();
            writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
            // body鍙傛暟鏀捐繖閲�
            writer.write(params);
            writer.flush();
            is = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            while ((strRead = reader.readLine()) != null) {
                sbf.append(strRead);
                sbf.append("\r\n");
            }
            results = sbf.toString();
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (Exception e) {
            }
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
            }
            try {
                if (is != null) {
                    is.close();
                }
            } catch (Exception e) {
            }
            try {
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Exception e) {
            }
        }
        return results;
    }

    /**
     * post请求的方式
     *
     * @param map
     * @return
     */
    public static String httpPost(HashMap map) {
        String content = "";
        try {
            String ruleName = map.get("ruleName").toString().trim();
            String requestPath = map.get("requestPath").toString().trim();
            String url = requestPath + ruleName;
            System.out.println(url);
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url);
            if (map == null) {
                System.out.println("null");
                return "";
            }
            System.out.println("map=" + map);
            String params = JSON.toJSONString(map);
            System.out.println("params: " + params);
            StringBody contentBody = new StringBody(params, CharsetUtils.get("UTF-8"));
            // 以浏览器兼容模式访问,否则就算指定编码格式,中文文件名上传也会乱码
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

            builder.addPart("data", contentBody);
            HttpEntity entity = builder.build();
            httpPost.setEntity(entity);
            HttpResponse response = httpClient.execute(httpPost);
            if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
                HttpEntity entitys = response.getEntity();
                if (entitys != null) {
                    content = EntityUtils.toString(entitys);
                }
            }
            httpClient.getConnectionManager().shutdown();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("post request commit error" + e);
            System.out.println("post request to microservice failure, please check microservice");
        }
        return content;
    }

    public static String sendGet(String url, String param) {
        InputStream inputStream = null;
        String result = "";
        BufferedReader in = null;
        try {
            String urlNameString = url + "?" + param;
            URL realUrl = new URL(urlNameString);
            // 打开和URL之间的连接
            URLConnection connection = realUrl.openConnection();
            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 建立实际的连接 connection.connect();
            // 获取所有响应头字段
            Map<String, List<String>> map = connection.getHeaderFields();
            // 遍历所有的响应头字段
            for (String key : map.keySet()) {
                System.out.println(key + "--->" + map.get(key));
            }
            inputStream = connection.getInputStream();
            // 定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送GET请求出现异常！" + e);
            e.printStackTrace();
        }
        // 使用finally块来关闭输入流
        finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return result;
    }


    public static HttpHeaders getJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/json;charset=UTF-8"));
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        headers.add("Accept-Charset", "UTF-8");
        return headers;
    }



    /**
     *
     * @param url 链接地址
     * @param params 填充在url中的参数
     * @param useProxy 是否使用代理
     * @param socketTimeout 超时时间
     * @param proxyHost 代理地址
     * @param proxyPort 代理端口号
     * @return
     */
    public static String httpGet(String url, String params, String useProxy, int socketTimeout, String proxyHost, String proxyPort) {
        String requestUrl = url;
        if (StringUtil.isNotEmpty(params)) {
            requestUrl = url + "?" + params;
        }
        String respData = null;
        log.info("httpGet req is 【{}】", params);
        HttpRequest httpRequest = HttpRequest.get(requestUrl).timeout(socketTimeout).header("token","application/json");
        if ("Y".equalsIgnoreCase(useProxy)) {
            log.info(String.format("使用代理"));
            httpRequest.setProxy(new Proxy(Proxy.Type.HTTP,
                    new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort))));
        }
        respData = httpRequest.execute().body();
        log.info(String.format("HttpsUtil:httpGet | 请求信息：%s | 响应信息: %s", httpRequest.getUrl(), respData));
        return respData;
    }



    /**
     *
     * @param url 链接地址
     * @param params 填充在url中的参数
     * @param sendBodyData body
     * @param useProxy 是否使用代理
     * @param socketTimeout 超时时间
     * @param proxyHost 代理地址
     * @param proxyPort 代理端口号
     * @return
     */
    public static String httpPost(String url, String params, String sendBodyData, String useProxy, int socketTimeout, String proxyHost, String proxyPort) {
        String requestUrl = url;
        if (StringUtil.isNotEmpty(params)) {
            requestUrl = url + "?" + params;
        }
        String respData = null;
        log.info("httpPost req is 【{}】", sendBodyData);
        HttpRequest httpRequest = HttpRequest.post(requestUrl).timeout(socketTimeout).header("Content-Type", "application/json");
        if ("Y".equalsIgnoreCase(useProxy)) {
            log.info(String.format("使用代理"));
            httpRequest.setProxy(new Proxy(Proxy.Type.HTTP,
                    new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort))));
        }
        if (StringUtil.isNotEmpty(sendBodyData)) {
            httpRequest.body(sendBodyData);
        }
        respData = httpRequest.execute().body();
        log.info(String.format("HttpsUtil:httpPost | 请求信息：%s | 响应信息: %s", httpRequest.getUrl(), respData));
        return respData;
    }

    /**
     * 返回文件流
     * @param url 链接地址
     * @param params 填充在url中的参数
     * @param sendBodyData body
     * @param useProxy 是否使用代理
     * @param socketTimeout 超时时间
     * @param proxyHost 代理地址
     * @param proxyPort 代理端口号
     * @return
     */
    public static InputStream httpPostFileInputStream(String url, String params, String sendBodyData, String useProxy, int socketTimeout, String proxyHost, String proxyPort) {
        String requestUrl = url;
        if (StringUtil.isNotEmpty(params)) {
            requestUrl = url + "?" + params;
        }
        log.info("httpPost req is 【{}】", sendBodyData);
        HttpRequest httpRequest = HttpRequest.post(requestUrl).timeout(socketTimeout).header("Content-Type", "application/json");
        if ("Y".equalsIgnoreCase(useProxy)) {
            log.info(String.format("使用代理"));
            httpRequest.setProxy(new Proxy(Proxy.Type.HTTP,
                    new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort))));
        }
        if (StringUtil.isNotEmpty(sendBodyData)) {
            httpRequest.body(sendBodyData);
        }
        cn.hutool.http.HttpResponse response = httpRequest.execute();
        String contentType = response.header("Content-Type");
        if (contentType.startsWith("application/json")) {
            return null;
        } else {
            return response.bodyStream();
        }
    }
}
