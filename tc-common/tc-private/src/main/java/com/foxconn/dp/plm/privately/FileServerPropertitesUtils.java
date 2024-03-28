package com.foxconn.dp.plm.privately;


import java.io.*;
import java.util.Locale;
import java.util.Properties;

/**
 * fileservice 配置信息管理类
 */
public class FileServerPropertitesUtils {

    public static Properties props= new Properties();

    static {
        try {
            String path= getCurrentPath(FileServerPropertitesUtils.class);
            readPropertiesFile(path,"fileService");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取配置文件李的配置信息，保存到props
     * @param filePath
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static Properties readPropertiesFile(String filePath,String fileName) throws Exception {
        InputStream inputStream = null;
        try {
            String newFileName = filePath +File.separator+ fileName+".properties";
            inputStream = new FileInputStream(newFileName);
            props.load(inputStream);
            return props;
        } catch (FileNotFoundException  e) {
            e.printStackTrace();
            return null;
        }catch (IOException ex){
            ex.printStackTrace();
            return null;
        }finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            }catch (IOException e){}
        }
    }

    /**
     * 从props取配置信息
     * @param key
     * @return
     */
    public static  String getProperty(String key){
        return  props.getProperty(key);
    }

    /**
     * 读取配置文件存放位置，配置文件要和jar文件放在同一个文件夹下
     * @param cls
     * @return
     */
    private static String getCurrentPath(Class<?> cls)
    {
       //获取jar包所在绝对路径
        String path = cls.getProtectionDomain().getCodeSource().getLocation().getPath();
        path = path.replaceFirst("file:/", "");
        path = path.replaceAll("!/", "");
        if (path.lastIndexOf(File.separator) >= 0) {
            path = path.substring(0, path.lastIndexOf(File.separator));
        }
        if (path.substring(0, 1).equalsIgnoreCase("/")) {
            String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
            if (osName.indexOf("window") >= 0) {
                path = path.substring(1);
            }
        }
        if(path.indexOf(".jar")>-1){
            path=path.substring(0,path.indexOf(".jar"));
            path=path.substring(0,path.lastIndexOf("/"));
        }
        String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);

        if (osName.indexOf("window") < 0) {
            if(!path.startsWith(File.separator)){
                 path= File.separator+path;
            }
        }

        return path;
    }



}
