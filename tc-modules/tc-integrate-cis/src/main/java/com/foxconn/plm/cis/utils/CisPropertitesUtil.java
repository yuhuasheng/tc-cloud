package com.foxconn.plm.cis.utils;

import org.springframework.core.io.ClassPathResource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

public class CisPropertitesUtil {

    public static Properties props;

    static {
        try {
            props= readPropertiesFile("/cisclassificationmapping.properties");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static Properties readPropertiesFile(String filePath) throws FileNotFoundException, IOException {
        InputStream inputStream = null;
        Properties props = null;
        try {
            ClassPathResource classPathResource = new ClassPathResource(filePath);
            inputStream = classPathResource.getInputStream();
            props = new Properties();
            props.load(new InputStreamReader(inputStream, "UTF-8"));
            return props;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            }catch(IOException e){}
        }
    }


}
