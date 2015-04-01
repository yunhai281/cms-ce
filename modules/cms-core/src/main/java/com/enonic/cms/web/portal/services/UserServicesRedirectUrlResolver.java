/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.web.portal.services;

import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

import com.enonic.esl.containers.MultiValueMap;
import com.enonic.esl.net.URL;

import com.enonic.cms.core.Attribute;
import com.enonic.cms.core.portal.httpservices.HttpServicesException;
import com.enonic.cms.core.portal.httpservices.IllegalRedirectException;
import com.enonic.cms.core.structure.SitePath;

@Component
public class UserServicesRedirectUrlResolver
{

    public String resolveRedirectUrlToPage( HttpServletRequest request, String redirect, MultiValueMap queryParams )
    {
        if ( redirect == null || redirect.length() == 0 )
        {
            String referer = request.getHeader( "referer" );
            if ( referer != null && !referer.equals( "" ) )
            {
                return appendParams( referer, queryParams );
            }
            return appendParams( "/", queryParams );
        }

        if ( redirect.contains( "://" ) )
        {
            return appendParams( redirect, queryParams );
        }

        if ( redirect.startsWith( "/" ) )
        {
            return appendParams( redirect, queryParams );
        }

        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append( "Requested redirect Url: " );
        errorMessage.append( redirect );
        String referer = request.getHeader( "referer" );
        if ( referer != null && !referer.equals( "" ) )
        {
            errorMessage.append( " - Referer: " );
            errorMessage.append( appendParams( referer, queryParams ) );
        }
        throw new IllegalRedirectException( errorMessage.toString() );
    }

    public String resolveRedirectUrlToErrorPage( HttpServletRequest request, Integer[] codes, ServicesProcessor errorSource )
    {

        // Check for a fatal exception
        for ( int code : codes )
        {
            if ( code >= 500 )
            {
                throw new HttpServicesException( code );
            }
        }

        SitePath originalSitePath = (SitePath) request.getAttribute( Attribute.ORIGINAL_SITEPATH );
        String handler = UserServicesParameterResolver.resolveHandlerFromSitePath( originalSitePath );
        String operation = UserServicesParameterResolver.resolveOperationFromSitePath( originalSitePath );

        Integer httpResponseCode = errorSource.httpResponseCodeTranslator( codes );

        // set error paramater on query string
        StringBuilder errorKeyBuilder = new StringBuilder( "error_" );
        if ( handler != null && operation != null )
        {
            errorKeyBuilder.append( handler );
            errorKeyBuilder.append( '_' );
            errorKeyBuilder.append( operation );
        }
        else
        {
            errorKeyBuilder.append( "userservices" );
        }

        String errorKey = errorKeyBuilder.toString();

        String baseUrlString = request.getHeader( "referer" );

        if ( baseUrlString == null )
        {
            throw new HttpServicesException( codes[0] );
        }

        // remove old error-parameters
        URL url = new URL( baseUrlString );
        removeErrorParameters( url );

        // add query parameters to url
        for ( int code : codes )
        {
            url.addParameter( errorKey, String.valueOf( code ) );
        }

        url.addParameter( "httpResponseCode", httpResponseCode.toString() );

        return url.toString();
    }

    private void removeErrorParameters( URL url )
    {
        Iterator paramIterator = url.parameterIterator();
        while ( paramIterator.hasNext() )
        {
            URL.Parameter param = (URL.Parameter) paramIterator.next();
            if ( param.getKey().contains( "error" ) )
            {
                paramIterator.remove();
            }
        }
    }


    private String appendParams( String urlString, MultiValueMap queryParams )
    {
        URL url = new URL( urlString );
        if ( queryParams != null && queryParams.size() > 0 )
        {
            for ( Object key : queryParams.keySet() )
            {
                for ( Object o : ( queryParams.getValueList( key ) ) )
                {
                    url.addParameter( key.toString(), o.toString() );
                }
            }
        }

        return url.toString();
    }
}
