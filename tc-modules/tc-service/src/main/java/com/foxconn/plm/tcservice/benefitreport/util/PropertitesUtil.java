package com.foxconn.plm.tcservice.benefitreport.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import com.foxconn.plm.utils.math.MathUtil;
import org.springframework.core.io.ClassPathResource;

import static com.foxconn.plm.tcservice.benefitreport.constant.BenefitFilePathConstant.CONFIGPATH;

public class PropertitesUtil {
    public static Properties props;

    static {
        try {
            props=readPropertiesFile(MathUtil.base64De(CONFIGPATH));
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
