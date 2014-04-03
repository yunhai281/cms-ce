/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal.ticket;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public final class TicketValidator
    implements TicketConstants
{
    public static boolean isValid( final HttpServletRequest request )
    {
        final String ticket = resolve( request );
        if ( ticket == null )
        {
            // no ticket is a invalid ticket
            return false;
        }

        final HttpSession session = request.getSession();
        final String storedTicket = (String) session.getAttribute( SESSION_VALUE_KEY );
        return ticket.equals( storedTicket );
    }

    private static String resolve( final HttpServletRequest request )
    {
        final String enctype = request.getContentType();

        if ( enctype != null && enctype.startsWith( "multipart/form-data" ) )
        {
            final Map queryValues = request.getParameterMap();
            if ( queryValues.containsKey( PARAMETER_NAME ) )
            {
                return ( (String[]) queryValues.get( PARAMETER_NAME ) )[0];
            }
        }
        else
        {
            return request.getParameter( PARAMETER_NAME );
        }

        return null;
    }
}
