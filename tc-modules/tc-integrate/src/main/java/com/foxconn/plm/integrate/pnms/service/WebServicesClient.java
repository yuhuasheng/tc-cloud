package com.foxconn.plm.integrate.pnms.service;

import cn.hutool.core.lang.Assert;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.integrate.pnms.client.PNMSWebServiceExceptionException;
import com.foxconn.plm.integrate.pnms.client.PNMSWebServiceStub;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.rmi.RemoteException;

@Service
public class WebServicesClient {
    private static Log log = LogFactory.get();

    @Value("${pnms.url}")
    private String url;

    @Value("${pnms.prodUrl:http://pnm.efoxconn.com/Windchill/servlet/services/PNMSWebService?wsdl}")
    private String prodUrl;


    public String queryHHPnByEnv(String hhpn, boolean isProd) {
        String pnmsUrl = isProd ? prodUrl : url;
        log.info("queryHHPnByEnv pnmsUrl -->> " + pnmsUrl);
        PNMSWebServiceStub.GetHHPNInfo info = new PNMSWebServiceStub.GetHHPNInfo();
        info.setHhpn(hhpn);
        PNMSWebServiceStub.GetHHPNInfoResponse response = null;
        try {
            response = PNMSWebServiceStub.getStub(pnmsUrl).getHHPNInfo(info);
        } catch (RemoteException | PNMSWebServiceExceptionException e) {
            e.printStackTrace();
        }
        if (response != null) {
            return response.get_return();
        } else {
            return null;
        }
    }

    public String queryHHPn(String hhpn) {
        log.info("url -->> " + url);
        PNMSWebServiceStub.GetHHPNInfo info = new PNMSWebServiceStub.GetHHPNInfo();
        info.setHhpn(hhpn);
        PNMSWebServiceStub.GetHHPNInfoResponse response = null;
        try {
            response = PNMSWebServiceStub.getStub(url).getHHPNInfo(info);
        } catch (RemoteException | PNMSWebServiceExceptionException e) {
            e.printStackTrace();
        }
        if (response != null) {
            return response.get_return();
        } else {
            return null;
        }
    }

    public String queryHHPn(String mfg, String mfgPn) {
        log.info("url -->> " + url);
        PNMSWebServiceStub.QueryFoxconnPNInfo info = new PNMSWebServiceStub.QueryFoxconnPNInfo();
        info.setMfg(mfg);
        info.setMfgPN(mfgPn);
        PNMSWebServiceStub.QueryFoxconnPNInfoResponse response = null;
        try {
            response = PNMSWebServiceStub.getStub(url).queryFoxconnPNInfo(info);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if (response != null) {
            return response.get_return();
        } else {
            return null;
        }

    }
}
