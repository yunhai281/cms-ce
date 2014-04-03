package com.enonic.cms.core.portal.ticket;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import junit.framework.Assert;

public class TicketValidatorTest
{
    @Test
    public void testValidateNotInSession()
    {
        final MockHttpServletRequest req = new MockHttpServletRequest();
        Assert.assertFalse( TicketValidator.isValid( req ) );
    }

    @Test
    public void testValidateNotInRequest()
    {
        final MockHttpServletRequest req = new MockHttpServletRequest();
        TicketGenerator.getOrGenerate( req );

        Assert.assertFalse( TicketValidator.isValid( req ) );
    }

    @Test
    public void testValidateWrongTicket()
    {
        final MockHttpServletRequest req = new MockHttpServletRequest();
        TicketGenerator.getOrGenerate( req );

        req.setAttribute( TicketConstants.PARAMETER_NAME, "123" );
        Assert.assertFalse( TicketValidator.isValid( req ) );
    }

    @Test
    public void testValidateRightTicket()
    {
        final MockHttpServletRequest req = new MockHttpServletRequest();
        final String ticket = TicketGenerator.getOrGenerate( req );

        req.setAttribute( TicketConstants.PARAMETER_NAME, ticket );
        Assert.assertFalse( TicketValidator.isValid( req ) );
    }
}
