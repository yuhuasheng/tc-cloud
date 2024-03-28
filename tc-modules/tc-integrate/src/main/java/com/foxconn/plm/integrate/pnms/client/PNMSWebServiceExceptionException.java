/**
 * PNMSWebServiceExceptionException.java
 * <p>
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.7.9  Built on : Nov 16, 2018 (12:05:37 GMT)
 */
package com.foxconn.plm.integrate.pnms.client;

public class PNMSWebServiceExceptionException extends Exception {
    private static final long serialVersionUID = 1672827717502L;
    private PNMSWebServiceStub.PNMSWebServiceException faultMessage;

    public PNMSWebServiceExceptionException() {
        super("PNMSWebServiceExceptionException");
    }

    public PNMSWebServiceExceptionException(String s) {
        super(s);
    }

    public PNMSWebServiceExceptionException(String s,
                                            Throwable ex) {
        super(s, ex);
    }

    public PNMSWebServiceExceptionException(Throwable cause) {
        super(cause);
    }

    public void setFaultMessage(
            PNMSWebServiceStub.PNMSWebServiceException msg) {
        faultMessage = msg;
    }

    public PNMSWebServiceStub.PNMSWebServiceException getFaultMessage() {
        return faultMessage;
    }
}
