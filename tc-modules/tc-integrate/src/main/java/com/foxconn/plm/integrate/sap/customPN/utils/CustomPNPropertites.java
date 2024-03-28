package com.foxconn.plm.integrate.sap.customPN.utils;

import org.springframework.core.io.ClassPathResource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

public class CustomPNPropertites {

    public static Properties props;

    static {
        try {
            props=readPropertiesFile("/customPN.properties");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Properties readPropertiesFile(String filePath) throws FileNotFoundException, IOException {
        InputStream inputStream = null;
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
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

}
