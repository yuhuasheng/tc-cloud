//==================================================
// 
//  Copyright 2017 Siemens Product Lifecycle Management Software Inc. All Rights Reserved.
//
//==================================================

package com.foxconn.plm.tcapi.soa.client;

import com.teamcenter.schemas.soa._2006_03.exceptions.InvalidCredentialsException;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.strong.core.SessionService;
import com.teamcenter.services.strong.core._2006_03.Session.LoginResponse;
import com.teamcenter.soa.SoaConstants;
import com.teamcenter.soa.client.Connection;
import com.teamcenter.soa.client.model.strong.User;
import com.teamcenter.soa.exceptions.CanceledOperationException;


public class AppXSession {
    /**
     * Single instance of the Connection object that is shared throughout
     * the application. This Connection object is needed whenever a Service
     * stub is instantiated.
     */
    private Connection connection;

    /**
     * The credentialManager is used both by the Session class and the Teamcenter
     * Services Framework to get user credentials.
     */
    private static AppXCredentialManager credentialManager;

    /**
     * Create an instance of the Session with a connection to the specified
     * server.
     * <p>
     * Add implementations of the ExceptionHandler, PartialErrorListener,
     * ChangeListener, and DeleteListeners.
     *
     * @param host Address of the host to connect to, http://serverName:port/tc
     */
    public AppXSession(String host) {
        // Create an instance of the CredentialManager, this is used
        // by the SOA Framework to get the user's credentials when
        // challenged by the server (session timeout on the web tier).
        credentialManager = new AppXCredentialManager();

        String protocol = null;
        String envNameTccs = null;
        if (host.startsWith("http")) {
            protocol = SoaConstants.HTTP;
        } else if (host.startsWith("tccs")) {
            protocol = SoaConstants.TCCS;
            host = host.trim();
            int envNameStart = host.indexOf('/') + 2;
            envNameTccs = host.substring(envNameStart, host.length());
            host = "";
        } else {
            protocol = SoaConstants.IIOP;
        }


        // Create the Connection object, no contact is made with the server
        // until a service request is made
        connection = new Connection(host, credentialManager, SoaConstants.REST, protocol);

        if (protocol == SoaConstants.TCCS) {
            connection.setOption(Connection.TCCS_ENV_NAME, envNameTccs);
        }


        // Add an ExceptionHandler to the Connection, this will handle any
        // InternalServerException, communication errors, XML marshaling errors
        // .etc
        connection.setExceptionHandler(new AppXExceptionHandler());

        // While the above ExceptionHandler is required, all of the following
        // Listeners are optional. Client application can add as many or as few Listeners
        // of each type that they want.

        // Add a Partial Error Listener, this will be notified when ever a
        // a service returns partial errors.
        connection.getModelManager().addPartialErrorListener(new AppXPartialErrorListener());

        // Add a Change and Delete Listener, this will be notified when ever a
        // a service returns model objects that have been updated or deleted.
        connection.getModelManager().addModelEventListener(new AppXModelEventListener());


        // Add a Request Listener, this will be notified before and after each
        // service request is sent to the server.
        Connection.addRequestListener(new AppXRequestListener());

    }

    /**
     * Get the single Connection object for the application
     *
     * @return connection
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Login to the Teamcenter Server
     */
    public User login() {
        // Get the service stub
        SessionService sessionService = SessionService.getService(connection);

        try {
            String[] credentials = credentialManager.promptForCredentials();
            while (true) {
                try {
                    LoginResponse out = sessionService.login(credentials[0], credentials[1],
                            credentials[2], credentials[3], "", credentials[4]);

                    return out.user;
                } catch (InvalidCredentialsException e) {
                    credentials = credentialManager.getCredentials(e);
                }
            }
        } catch (CanceledOperationException e) {
        }
        System.exit(0);
        return null;
    }

    public User login(String username, String password, String usergroup, String userrole) {
        SessionService sessionService = SessionService.getService(connection);
        {
            try {
                LoginResponse resp = sessionService.login(username, password, usergroup, userrole, "", "");
                return resp.user;
            } catch (InvalidCredentialsException e) {
                return null;
            }
        }
    }


    public void logout() {
        if (connection != null) {
            SessionService sessionService = SessionService.getService(connection);
            try {
                sessionService.logout();
            } catch (ServiceException e) {
            }
        }
    }

}
