package com.enonic.cms.core.portal.ticket;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public final class TicketGenerator
    implements TicketConstants
{
    public static String getOrGenerate( final HttpServletRequest request )
    {
        final HttpSession session = request.getSession();
        final String oldTicket = (String) session.getAttribute( SESSION_VALUE_KEY );

        if ( oldTicket != null )
        {
            return oldTicket;
        }

        final String ticket = UUID.randomUUID().toString().replace( "-", "" );
        session.setAttribute( SESSION_VALUE_KEY, ticket );
        return ticket;
    }
}
