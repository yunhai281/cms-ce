package com.enonic.cms.core.portal.ticket;

import javax.servlet.http.HttpSession;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import junitx.framework.Assert;

public class TicketGeneratorTest
{
    @Test
    public void testGenerate()
    {
        final MockHttpServletRequest req = new MockHttpServletRequest();
        final HttpSession session = req.getSession();

        Assert.assertNull( session.getAttribute( TicketConstants.SESSION_VALUE_KEY ) );

        final String ticket1 = TicketGenerator.getOrGenerate( req );
        Assert.assertEquals( ticket1, session.getAttribute( TicketConstants.SESSION_VALUE_KEY ) );

        final String ticket2 = TicketGenerator.getOrGenerate( req );
        Assert.assertEquals( ticket1, ticket2 );
    }
}
