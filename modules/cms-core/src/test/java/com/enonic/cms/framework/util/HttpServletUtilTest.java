/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.framework.util;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import junit.framework.TestCase;

import com.enonic.esl.util.DigestUtil;


public class HttpServletUtilTest
    extends TestCase
{

    public void testSetDateHeader()
    {
        GregorianCalendar cal = new GregorianCalendar( TimeZone.getTimeZone( "GMT" ) );
        cal.set( 1994, Calendar.DECEMBER, 1, 16, 0, 0 );
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();

        HttpServletUtil.setDateHeader( mockResponse, cal.getTime() );
        assertEquals( "Thu, 01 Dec 1994 16:00:00 GMT", mockResponse.getHeader( "Date" ) );
    }

    public void testSetExpiresHeader()
    {
        GregorianCalendar cal = new GregorianCalendar( TimeZone.getTimeZone( "GMT" ) );
        cal.set( 1994, Calendar.DECEMBER, 1, 16, 0, 0 );
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();

        HttpServletUtil.setExpiresHeader( mockResponse, cal.getTime() );
        assertEquals( "Thu, 01 Dec 1994 16:00:00 GMT", mockResponse.getHeader( "Expires" ) );
    }

    public void testSetExpiresHeaderConvertsLocalTimeToGMT()
    {
        GregorianCalendar cal = new GregorianCalendar( TimeZone.getTimeZone( "Europe/Oslo" ) );
        cal.set( 1994, Calendar.DECEMBER, 1, 16, 0, 0 );
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();

        HttpServletUtil.setExpiresHeader( mockResponse, cal.getTime() );
        assertEquals( "Thu, 01 Dec 1994 15:00:00 GMT", mockResponse.getHeader( "Expires" ) );
    }

    public void testIsContentModifiedAccordingToIfNoneMatchHeader()
    {
        String etagFor123 = DigestUtil.generateSHA( "123" );
        String etagFor321 = DigestUtil.generateSHA( "321" );

        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader( "If-None-Match", etagFor123 );

        assertFalse( HttpServletUtil.isContentModifiedAccordingToIfNoneMatchHeader( mockRequest, etagFor123 ) );
        assertTrue( HttpServletUtil.isContentModifiedAccordingToIfNoneMatchHeader( mockRequest, etagFor321 ) );
    }

    public void testSchemeUsingForwardedHeader() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest(  );
        mockRequest.addHeader( "Forwarded", "for=192.0.2.60;proto=https;by=203.0.113.43" );
        mockRequest.addHeader( "X-Forwarded-Proto", "smtp" );
        mockRequest.setScheme( "http" );
        assertEquals( "https", HttpServletUtil.getScheme( mockRequest ));
    }

    public void testSchemeUsingXForwardedProtoHeader() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest(  );
        mockRequest.addHeader( "X-Forwarded-Proto", "https" );
        mockRequest.setScheme( "http" );
        assertEquals( "https", HttpServletUtil.getScheme( mockRequest ));
    }

    /*
     * If Forwarded is used, X-Forwarded-Proto should be ignored, even if proto is not set for Forwarded.
     */
    public void testSchemeWithoutAnyForwardedHeader() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest(  );
        mockRequest.addHeader( "Forwarded", "for=192.0.2.43, for=198.51.100.17" );
        mockRequest.addHeader( "X-Forwarded-Proto", "smtp" );
        mockRequest.setScheme( "http" );
        assertEquals( "http", HttpServletUtil.getScheme( mockRequest ));
    }
}
