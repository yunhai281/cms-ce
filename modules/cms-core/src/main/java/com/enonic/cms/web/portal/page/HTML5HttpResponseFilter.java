/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.web.portal.page;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import com.enonic.cms.api.plugin.ext.http.HttpResponseFilter;

/**
 * forces HTML5 for doctypeHandler = "html5forcer"
 * <p/>
 * removes about:legacy-compat and other XSLT things from doctype for doctypeHandler = "html5fixer"
 * <p/>
 * does nothing in other cases
 */
public final class HTML5HttpResponseFilter
    extends HttpResponseFilter
{
    private static final Pattern DOCTYPE =
        Pattern.compile( "^(\\s*)(<!DOCTYPE .*?>)(.*)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL );

    private static final String DOCTYPE_HTML = "<!DOCTYPE html>";

    private final String doctypeHandler;

    public HTML5HttpResponseFilter( final String doctypeHandler )
    {
        this.doctypeHandler = doctypeHandler;
    }

    @Override
    public String filterResponse( final HttpServletRequest request, final String response, final String contentType )
        throws Exception
    {
        if ( "none".equals( doctypeHandler ) || StringUtils.isEmpty( doctypeHandler ) )
        {
            return response;
        }

        // handle only text/html
        if ( contentType == null || !contentType.startsWith( "text/html" ) )
        {
            return response;
        }


        final Matcher matcher = DOCTYPE.matcher( response );

        if ( matcher.find() )
        {
            final String doctype = matcher.group( 2 );

            if ( "html5fixer".equals( doctypeHandler ) && doctype.contains( "\"about:legacy-compat\"" ) ||
                "html5forcer".equals( doctypeHandler ) && !doctype.toLowerCase().contains( "frameset" ) )
            {
                return DOCTYPE_HTML + matcher.group( 3 );
            }
        }

        return response;
    }
}
