package com.foxconn.plm.integrate.sap.customPN.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import com.sap.conn.jco.ext.DestinationDataProvider;

public class ConnectPoolUtils {

	public static String ABAP_AS_POOLED = "ABAP_AS_WITH_POOL";
	public static String ABAP_AS_POOLED_888 = "ABAP_AS_WITH_POOL_888";
	public static String ABAP_AS_POOLED_868 = "ABAP_AS_WITH_POOL_868";
	static {

		Properties connectProperties = new Properties();
		connectProperties.setProperty(DestinationDataProvider.JCO_ASHOST,
				SAPConstants.SAP_IP);
		connectProperties.setProperty(DestinationDataProvider.JCO_SYSNR,
				SAPConstants.SAP_SYSTEMNUMBER);
		connectProperties.setProperty(DestinationDataProvider.JCO_CLIENT,
				SAPConstants.SAP_SITE);
		connectProperties.setProperty(DestinationDataProvider.JCO_USER,
				SAPConstants.SAP_USERID);
		connectProperties.setProperty(DestinationDataProvider.JCO_PASSWD,
				SAPConstants.getSapSD());
		connectProperties.setProperty(DestinationDataProvider.JCO_LANG,
				SAPConstants.SAP_LANGUAGE);

		connectProperties.setProperty(
				DestinationDataProvider.JCO_POOL_CAPACITY,
				SAPConstants.SAP_ALIVE_CONNECT);

		// JCO_POOL_CAPACITY - Maximum number of active connections that
		connectProperties.setProperty(DestinationDataProvider.JCO_PEAK_LIMIT,
				SAPConstants.SAP_MAX_CONNECT);
		createDataFile(ABAP_AS_POOLED, "jcoDestination", connectProperties);

		Properties connectProperties888 = new Properties();
		connectProperties888.setProperty(DestinationDataProvider.JCO_ASHOST,
				SAPConstants.SAP_IP_888);
		connectProperties888.setProperty(DestinationDataProvider.JCO_SYSNR,
				SAPConstants.SAP_SYSTEMNUMBER_888);
		connectProperties888.setProperty(DestinationDataProvider.JCO_CLIENT,
				SAPConstants.SAP_SITE_888);
		connectProperties888.setProperty(DestinationDataProvider.JCO_USER,
				SAPConstants.SAP_USERID_888);
		connectProperties888.setProperty(DestinationDataProvider.JCO_PASSWD,
				SAPConstants.getSapSD888());
		connectProperties888.setProperty(DestinationDataProvider.JCO_LANG,
				SAPConstants.SAP_LANGUAGE);
		// JCO_PEAK_LIMIT - Maximum number of idle connections kept open by the
		connectProperties888.setProperty(
				DestinationDataProvider.JCO_POOL_CAPACITY,
				SAPConstants.SAP_ALIVE_CONNECT);
		// JCO_POOL_CAPACITY - Maximum number of active connections that
		connectProperties888.setProperty(DestinationDataProvider.JCO_PEAK_LIMIT,
				SAPConstants.SAP_MAX_CONNECT);
		createDataFile(ABAP_AS_POOLED_888, "jcoDestination", connectProperties888);


		Properties connectProperties868 = new Properties();
		connectProperties868.setProperty(DestinationDataProvider.JCO_ASHOST,
				SAPConstants.SAP_IP_868);
		connectProperties868.setProperty(DestinationDataProvider.JCO_SYSNR,
				SAPConstants.SAP_SYSTEMNUMBER_868);
		connectProperties868.setProperty(DestinationDataProvider.JCO_CLIENT,
				SAPConstants.SAP_SITE_868);
		connectProperties868.setProperty(DestinationDataProvider.JCO_USER,
				SAPConstants.SAP_USERID_868);
		connectProperties868.setProperty(DestinationDataProvider.JCO_PASSWD,
				SAPConstants.getSapSD868());
		connectProperties868.setProperty(DestinationDataProvider.JCO_LANG,
				"EN");
		connectProperties868.setProperty(
				DestinationDataProvider.JCO_POOL_CAPACITY,
				SAPConstants.SAP_ALIVE_CONNECT);
		connectProperties868.setProperty(DestinationDataProvider.JCO_PEAK_LIMIT,
				SAPConstants.SAP_MAX_CONNECT);
		createDataFile(ABAP_AS_POOLED_868, "jcoDestination", connectProperties868);


	}


	
	static void createDataFile(String name, String suffix, Properties properties) {
		File cfg = new File(name + "." + suffix);
		FileOutputStream fos=null;
		if (!cfg.exists()) {
			try {
				 fos = new FileOutputStream(cfg, false);
				 properties.store(fos, "initial success !");
			} catch (Exception e) {
				throw new RuntimeException(
						"Unable to create the destination file "
								+ cfg.getName(), e);
			}finally {
				try {
				     if(fos !=null){
						fos.close();
				      }
				}catch (IOException e){}
			}
		}
	}
	
	
	

}
