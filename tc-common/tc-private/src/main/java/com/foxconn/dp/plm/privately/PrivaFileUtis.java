package com.foxconn.dp.plm.privately;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

public class PrivaFileUtis {


    public static String  getTmpdir( ){
        String tmpdir = System.getProperty("java.io.tmpdir");
        return tmpdir;
    }


    public static File getFile(String localPath ){
        return new File(localPath);
    }
    public static  File getFile(String localPath ,String tmpPath){
        return new File(localPath+tmpPath);
    }
    public static  File getFile(String path,String path2,String path3,String extType ){
        return new File(path+path2+path3+extType);
    }

    public static FileOutputStream getFileOutputStream(String path, String path2, String path3, String extType) throws FileNotFoundException {
        return new FileOutputStream(path+path2+path3+extType);
    }




    public static File releaseFile(String fileName) throws Exception {
        ClassPathResource classPathResource = new ClassPathResource(fileName);

        InputStream inputStream = classPathResource.getInputStream();
        try {
            String outDir = System.getProperty("user.dir") + "/release/";
            File dirFile = new File(outDir);
            if (!dirFile.exists()) {
                dirFile.mkdirs();
            }
            File destination = new File(outDir + fileName);
            FileUtils.copyInputStreamToFile(inputStream, destination);
            return destination;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return null;
    }


    public static String  getUrl(){
        return System.getProperty("java.io.tmpdir");
    }

}
