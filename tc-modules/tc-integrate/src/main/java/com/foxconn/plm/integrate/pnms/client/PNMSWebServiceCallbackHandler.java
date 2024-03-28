/**
 * PNMSWebServiceCallbackHandler.java
 * <p>
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.7.9  Built on : Nov 16, 2018 (12:05:37 GMT)
 */
package com.foxconn.plm.integrate.pnms.client;


/**
 *  PNMSWebServiceCallbackHandler Callback class, Users can extend this class and implement
 *  their own receiveResult and receiveError methods.
 */
public abstract class PNMSWebServiceCallbackHandler {
    protected Object clientData;

    /**
     * User can pass in any object that needs to be accessed once the NonBlocking
     * Web service call is finished and appropriate method of this CallBack is called.
     * @param clientData Object mechanism by which the user can pass in user data
     * that will be avilable at the time this callback is called.
     */
    public PNMSWebServiceCallbackHandler(Object clientData) {
        this.clientData = clientData;
    }

    /**
     * Please use this constructor if you don't want to set any clientData
     */
    public PNMSWebServiceCallbackHandler() {
        this.clientData = null;
    }

    /**
     * Get the client data
     */
    public Object getClientData() {
        return clientData;
    }

    /**
     * auto generated Axis2 call back method for uploadFile method
     * override this method for handling normal response from uploadFile operation
     */
    public void receiveResultuploadFile(
            PNMSWebServiceStub.UploadFileResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from uploadFile operation
     */
    public void receiveErroruploadFile(Exception e) {
    }

    /**
     * auto generated Axis2 call back method for queryFoxconnPNInfo method
     * override this method for handling normal response from queryFoxconnPNInfo operation
     */
    public void receiveResultqueryFoxconnPNInfo(
            PNMSWebServiceStub.QueryFoxconnPNInfoResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from queryFoxconnPNInfo operation
     */
    public void receiveErrorqueryFoxconnPNInfo(Exception e) {
    }

    /**
     * auto generated Axis2 call back method for downloadFileByFoxconnPN method
     * override this method for handling normal response from downloadFileByFoxconnPN operation
     */
    public void receiveResultdownloadFileByFoxconnPN(
            PNMSWebServiceStub.DownloadFileByFoxconnPNResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from downloadFileByFoxconnPN operation
     */
    public void receiveErrordownloadFileByFoxconnPN(Exception e) {
    }

    /**
     * auto generated Axis2 call back method for getHHPNInfo method
     * override this method for handling normal response from getHHPNInfo operation
     */
    public void receiveResultgetHHPNInfo(
            PNMSWebServiceStub.GetHHPNInfoResponse result) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from getHHPNInfo operation
     */
    public void receiveErrorgetHHPNInfo(Exception e) {
    }
}
