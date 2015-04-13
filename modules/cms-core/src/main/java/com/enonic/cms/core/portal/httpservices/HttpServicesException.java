/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal.httpservices;

public class HttpServicesException
    extends RuntimeException
{

    private String message;

    public HttpServicesException( int errorCode )
    {
        this.message = "Error in http services, error code: " + errorCode;
    }

    public String getMessage()
    {
        return message;
    }

}
