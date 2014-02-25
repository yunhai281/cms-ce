/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.framework.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.common.net.HttpHeaders;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;

public class HttpServletRangeUtilTest
{
    @Test
    public void test_range_regular_expression()
        throws Exception
    {
        assertTrue( "range cannot be parsed!", "bytes=0-0,-1".matches( HttpServletRangeUtil.PATTERN_RANGE ) );
        assertTrue( "range cannot be parsed!", "bytes=0-5".matches( HttpServletRangeUtil.PATTERN_RANGE ) );
        assertTrue( "range cannot be parsed!", "bytes=0-5,6-10,11-20".matches( HttpServletRangeUtil.PATTERN_RANGE ) );
        assertTrue( "range cannot be parsed!", "bytes=0-5, 6-10, 11-20".matches( HttpServletRangeUtil.PATTERN_RANGE ) );
        assertTrue( "range cannot be parsed!", "bytes=-500".matches( HttpServletRangeUtil.PATTERN_RANGE ) );
        assertTrue( "range cannot be parsed!", "bytes=9500-".matches( HttpServletRangeUtil.PATTERN_RANGE ) );
        assertTrue( "range cannot be parsed!", "bytes=500-600,601-999".matches( HttpServletRangeUtil.PATTERN_RANGE ) );
    }

    @Test
    public void test_bad_symbols_in_range()
        throws Exception
    {
        final MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod( "GET" );
        httpServletRequest.setPathInfo( "/input.dat" );
        httpServletRequest.addHeader( HttpHeaders.RANGE, "bytes=bad" );

        final MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        HttpServletRangeUtil.processRequest( httpServletRequest, mockHttpServletResponse, "input.dat", "application/pdf", input() );

        assertEquals( "", mockHttpServletResponse.getContentAsString() );

        assertEquals( HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE, mockHttpServletResponse.getStatus() );
    }

    @Test
    public void test_bad_range()
        throws Exception
    {
        final MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod( "GET" );
        httpServletRequest.setPathInfo( "/input.dat" );
        httpServletRequest.addHeader( HttpHeaders.RANGE, "bytes=5-1" );

        final MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        HttpServletRangeUtil.processRequest( httpServletRequest, mockHttpServletResponse, "input.dat", "application/pdf", input() );

        assertEquals( "", mockHttpServletResponse.getContentAsString() );

        assertEquals( HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE, mockHttpServletResponse.getStatus() );
    }

    @Test
    public void test_out_of_range()
        throws Exception
    {
        final MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod( "GET" );
        httpServletRequest.setPathInfo( "/input.dat" );
        httpServletRequest.addHeader( HttpHeaders.RANGE, "bytes=50000-50100" );

        final MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        HttpServletRangeUtil.processRequest( httpServletRequest, mockHttpServletResponse, "input.dat", "application/pdf", input() );

        assertEquals( "", mockHttpServletResponse.getContentAsString() );

        assertEquals( HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE, mockHttpServletResponse.getStatus() );
    }

    @Test
    public void test_out_of_range_in_multipart()
        throws Exception
    {
        final MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod( "GET" );
        httpServletRequest.setPathInfo( "/input.dat" );
        httpServletRequest.addHeader( HttpHeaders.RANGE, "bytes=0-5, 50000-50100" );

        final MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        HttpServletRangeUtil.processRequest( httpServletRequest, mockHttpServletResponse, "input.dat", "application/pdf", input() );

        assertEquals( "", mockHttpServletResponse.getContentAsString() );

        assertEquals( HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE, mockHttpServletResponse.getStatus() );
    }


    @Test
    public void test_no_range()
        throws Exception
    {
        final MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod( "GET" );
        httpServletRequest.setPathInfo( "/input.dat" );

        final MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        HttpServletRangeUtil.processRequest( httpServletRequest, mockHttpServletResponse, "input.dat", "application/pdf", input() );

        assertEquals( readFromFile( "input.dat" ), mockHttpServletResponse.getContentAsString() );

        assertEquals( HttpServletResponse.SC_OK, mockHttpServletResponse.getStatus() );
        assertEquals( "52", mockHttpServletResponse.getHeader( HttpHeaders.CONTENT_LENGTH ) );
    }

    @Test
    public void test_process_request_multipart_zero_to_zero()
        throws Exception
    {
        final MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod( "GET" );
        httpServletRequest.setPathInfo( "/input.dat" );
        httpServletRequest.addHeader( HttpHeaders.RANGE, "bytes=0-0,-1" );

        final MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        HttpServletRangeUtil.processRequest( httpServletRequest, mockHttpServletResponse, "input.dat", "application/pdf", input() );

        assertEquals( readFromFile( "response1.dat" ), mockHttpServletResponse.getContentAsString() );

        assertEquals( HttpServletResponse.SC_PARTIAL_CONTENT, mockHttpServletResponse.getStatus() );
        assertEquals( "multipart/byteranges; boundary=THIS_STRING_SEPARATES", mockHttpServletResponse.getContentType() );
        assertEquals( "attachment;filename=\"input.dat\"", mockHttpServletResponse.getHeader( HttpHeaders.CONTENT_DISPOSITION ) );

    }

    @Test
    public void test_process_request_plain_one_range()
        throws Exception
    {
        final MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod( "GET" );
        httpServletRequest.setPathInfo( "/input.dat" );
        httpServletRequest.addHeader( HttpHeaders.RANGE, "bytes=0-5" );

        final MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        HttpServletRangeUtil.processRequest( httpServletRequest, mockHttpServletResponse, "input.dat", "application/pdf", input() );

        assertEquals( "AaBbCc", mockHttpServletResponse.getContentAsString() );

        assertEquals( HttpServletResponse.SC_PARTIAL_CONTENT, mockHttpServletResponse.getStatus() );
        assertEquals( "application/pdf", mockHttpServletResponse.getContentType() );
        assertEquals( "attachment;filename=\"input.dat\"", mockHttpServletResponse.getHeader( HttpHeaders.CONTENT_DISPOSITION ) );
        assertEquals( "6", mockHttpServletResponse.getHeader( HttpHeaders.CONTENT_LENGTH ) );

    }

    @Test
    public void test_process_request_multipart_three_ranges()
        throws Exception
    {
        final MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod( "GET" );
        httpServletRequest.setPathInfo( "/input.dat" );
        httpServletRequest.addHeader( HttpHeaders.RANGE, "bytes=0-5,6-10,11-20" );

        final MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        HttpServletRangeUtil.processRequest( httpServletRequest, mockHttpServletResponse, "input.dat", "application/pdf", input() );

        assertEquals( readFromFile( "response3.dat" ), mockHttpServletResponse.getContentAsString() );

        assertEquals( HttpServletResponse.SC_PARTIAL_CONTENT, mockHttpServletResponse.getStatus() );
        assertEquals( "multipart/byteranges; boundary=THIS_STRING_SEPARATES", mockHttpServletResponse.getContentType() );
        assertEquals( "attachment;filename=\"input.dat\"", mockHttpServletResponse.getHeader( HttpHeaders.CONTENT_DISPOSITION ) );

    }

    @Test
    public void test_process_request_multipart_three_ranges_with_spaces()
        throws Exception
    {
        final MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod( "GET" );
        httpServletRequest.setPathInfo( "/input.dat" );
        httpServletRequest.addHeader( HttpHeaders.RANGE, "bytes=0-5, 6-10,     11-20" );

        final MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        HttpServletRangeUtil.processRequest( httpServletRequest, mockHttpServletResponse, "input.dat", "application/pdf", input() );

        assertEquals( readFromFile( "response3.dat" ), mockHttpServletResponse.getContentAsString() );

        assertEquals( HttpServletResponse.SC_PARTIAL_CONTENT, mockHttpServletResponse.getStatus() );
        assertEquals( "multipart/byteranges; boundary=THIS_STRING_SEPARATES", mockHttpServletResponse.getContentType() );
        assertEquals( "attachment;filename=\"input.dat\"", mockHttpServletResponse.getHeader( HttpHeaders.CONTENT_DISPOSITION ) );
    }

    @Test
    public void test_process_request_plain_minus_range()
        throws Exception
    {
        final MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod( "GET" );
        httpServletRequest.setPathInfo( "/input.dat" );
        httpServletRequest.addHeader( HttpHeaders.RANGE, "bytes=-50" );

        final MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        HttpServletRangeUtil.processRequest( httpServletRequest, mockHttpServletResponse, "input.dat", "application/pdf", input() );

        assertEquals( "BbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwXxYyZz", mockHttpServletResponse.getContentAsString() );

        assertEquals( HttpServletResponse.SC_PARTIAL_CONTENT, mockHttpServletResponse.getStatus() );
        assertEquals( "application/pdf", mockHttpServletResponse.getContentType() );
        assertEquals( "attachment;filename=\"input.dat\"", mockHttpServletResponse.getHeader( HttpHeaders.CONTENT_DISPOSITION ) );
        assertEquals( "50", mockHttpServletResponse.getHeader( HttpHeaders.CONTENT_LENGTH ) );

    }

    @Test
    public void test_process_request_plain_some_range()
        throws Exception
    {
        final MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod( "GET" );
        httpServletRequest.setPathInfo( "/input.dat" );
        httpServletRequest.addHeader( HttpHeaders.RANGE, "bytes=-48" );

        final MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        HttpServletRangeUtil.processRequest( httpServletRequest, mockHttpServletResponse, "input.dat", "application/pdf", input() );

        assertEquals( "CcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwXxYyZz", mockHttpServletResponse.getContentAsString() );

        assertEquals( HttpServletResponse.SC_PARTIAL_CONTENT, mockHttpServletResponse.getStatus() );
        assertEquals( "application/pdf", mockHttpServletResponse.getContentType() );
        assertEquals( "attachment;filename=\"input.dat\"", mockHttpServletResponse.getHeader( HttpHeaders.CONTENT_DISPOSITION ) );
        assertEquals( "48", mockHttpServletResponse.getHeader( HttpHeaders.CONTENT_LENGTH ) );

    }

    @Test
    public void test_process_request_plain_range_minus()
        throws Exception
    {
        final MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod( "GET" );
        httpServletRequest.setPathInfo( "/input.dat" );
        httpServletRequest.addHeader( HttpHeaders.RANGE, "bytes=50-" );

        final MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        HttpServletRangeUtil.processRequest( httpServletRequest, mockHttpServletResponse, "input.dat", "application/pdf", input() );

        assertEquals( "Zz", mockHttpServletResponse.getContentAsString() );

        assertEquals( HttpServletResponse.SC_PARTIAL_CONTENT, mockHttpServletResponse.getStatus() );
        assertEquals( "application/pdf", mockHttpServletResponse.getContentType() );
        assertEquals( "attachment;filename=\"input.dat\"", mockHttpServletResponse.getHeader( HttpHeaders.CONTENT_DISPOSITION ) );
        assertEquals( "2", mockHttpServletResponse.getHeader( HttpHeaders.CONTENT_LENGTH ) );

    }

    @Test
    public void test_process_request_plain_last_range_minus()
        throws Exception
    {
        final MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod( "GET" );
        httpServletRequest.setPathInfo( "/input.dat" );
        httpServletRequest.addHeader( HttpHeaders.RANGE, "bytes=51-" );

        final MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        HttpServletRangeUtil.processRequest( httpServletRequest, mockHttpServletResponse, "input.dat", "application/pdf", input() );

        assertEquals( "z", mockHttpServletResponse.getContentAsString() );

        assertEquals( HttpServletResponse.SC_PARTIAL_CONTENT, mockHttpServletResponse.getStatus() );
        assertEquals( "application/pdf", mockHttpServletResponse.getContentType() );
        assertEquals( "attachment;filename=\"input.dat\"", mockHttpServletResponse.getHeader( HttpHeaders.CONTENT_DISPOSITION ) );
        assertEquals( "1", mockHttpServletResponse.getHeader( HttpHeaders.CONTENT_LENGTH ) );

    }

    @Test
    public void test_gzip_ranges()
        throws Exception
    {
        final MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod( "GET" );
        httpServletRequest.setPathInfo( "/input.js" );
        httpServletRequest.addHeader( HttpHeaders.RANGE, "bytes=0-0,-1" );
        httpServletRequest.addHeader( HttpHeaders.ACCEPT_ENCODING, "gzip" );

        final MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        HttpServletRangeUtil.processRequest( httpServletRequest, mockHttpServletResponse, "input.js", "application/javascript", input() );

        assertEquals( readFromFile( "response4.dat" ), mockHttpServletResponse.getContentAsString() );

        assertEquals( HttpServletResponse.SC_PARTIAL_CONTENT, mockHttpServletResponse.getStatus() );
        assertEquals( "multipart/byteranges; boundary=THIS_STRING_SEPARATES", mockHttpServletResponse.getContentType() );
        assertEquals( "inline;filename=\"input.js\"", mockHttpServletResponse.getHeader( HttpHeaders.CONTENT_DISPOSITION ) );
    }


    @Test
    public void test_gzip_plain()
        throws Exception
    {
        final MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod( "GET" );
        httpServletRequest.setPathInfo( "/input.js" );
        httpServletRequest.addHeader( HttpHeaders.ACCEPT_ENCODING, "gzip" );

        final MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        HttpServletRangeUtil.processRequest( httpServletRequest, mockHttpServletResponse, "input.js", "application/javascript", input() );

        assertTrue( mockHttpServletResponse.getContentAsByteArray().length > 0 );

        assertEquals( HttpServletResponse.SC_OK, mockHttpServletResponse.getStatus() );
        assertEquals( "application/javascript", mockHttpServletResponse.getContentType() );
        assertEquals( "inline;filename=\"input.js\"", mockHttpServletResponse.getHeader( HttpHeaders.CONTENT_DISPOSITION ) );

        assertEquals( null, mockHttpServletResponse.getHeader( HttpHeaders.CONTENT_LENGTH ) );
    }


    private String readFromFile( final String fileName )
        throws Exception
    {
        final URL url = getClass().getResource( fileName );
        if ( url == null )
        {
            throw new IllegalArgumentException( "Resource file [" + fileName + "] not found" );
        }

        return Resources.toString( url, Charsets.UTF_8 );
    }

    private File input()
        throws IOException
    {
        final URL url = getClass().getResource( "input.dat" );

        File file = new File( "input.dat" );
        FileUtils.writeStringToFile( file, Resources.toString( url, Charsets.UTF_8 ) );

        return file;
    }
}
