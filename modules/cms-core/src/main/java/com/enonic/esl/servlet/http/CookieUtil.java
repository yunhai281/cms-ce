/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.esl.servlet.http;

import java.util.ArrayList;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CookieUtil
{

    /**
     * Get a request's cookie. If no cookie with this name, the method returns null.
     *
     * @param request    HttpRequest the cookie to search for cookie
     * @param cookieName String a cookie name
     * @return Cookie the cookie found, null if not found
     */
    public static Cookie getCookie( HttpServletRequest request, String cookieName )
    {
        if ( request != null && request.getCookies() != null && cookieName != null && cookieName.length() > 0 )
        {
            Cookie[] cookies = request.getCookies();
            for ( Cookie c : cookies )
            {
                if ( cookieName.equals( c.getName() ) )
                {
                    return c;
                }
            }
        }
        return null;
    }

    /**
     * Get all cookies by the same name on the request.
     *
     * @param request    HttpRequest the cookie to search for cookie
     * @param cookieName String a cookie name
     * @return An array of cookies.  If no cookies found, the array will be empty.
     */
    public static ArrayList<Cookie> getCookies( HttpServletRequest request, String cookieName )
    {
        ArrayList<Cookie> found = new ArrayList<Cookie>(  );
        if ( request != null && request.getCookies() != null && cookieName != null && cookieName.length() > 0 )
        {
            Cookie[] cookies = request.getCookies();
            for ( Cookie c : cookies )
            {
                if ( cookieName.equals( c.getName() ) )
                {
                    found.add( c );
                }
            }
        }
        return found;
    }

    public static void setCookie( HttpServletResponse response, String cookieName, String value, int maxAge, String path )
    {
        Cookie cookie = new Cookie( cookieName, value );
        cookie.setMaxAge( maxAge );
        cookie.setPath( getCookiePath( path ) );
        response.addCookie( cookie );
    }

    private static String getCookiePath( String path )
    {
        final String pathSeparator = "/";
        String cookiePath = pathSeparator;
        if ( path != null && path.length() > 0 )
        {
            cookiePath = path;

            if ( !cookiePath.startsWith( pathSeparator ) )
            {
                cookiePath = pathSeparator + cookiePath;
            }
            if ( !cookiePath.endsWith( pathSeparator ) )
            {
                cookiePath = cookiePath + pathSeparator;
            }
        }
        return cookiePath;
    }
}
