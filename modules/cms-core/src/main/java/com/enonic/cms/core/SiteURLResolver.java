/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core;

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.enonic.cms.framework.util.HttpServletUtil;
import com.enonic.cms.framework.util.UrlPathEncoder;

import com.enonic.cms.core.structure.SiteKey;
import com.enonic.cms.core.structure.SitePath;
import com.enonic.cms.core.structure.SitePropertiesService;
import com.enonic.cms.core.structure.SitePropertyNames;
import com.enonic.cms.core.vhost.VirtualHostHelper;

/**
 * SiteURLResolver is somewhere spring bean, somewhere it is not .
 *
 * @see com.enonic.cms.core.portal.rendering.PageRenderer
 * @see com.enonic.cms.core.portal.rendering.WindowRenderer
 */
@Component
public class SiteURLResolver
{
    private final static int DEFAULT_HTTP_PORT = 80;

    private final static int DEFAULT_HTTPS_PORT = 443;

    public static final String DEFAULT_SITEPATH_PREFIX = "/site";

    private String characterEncoding;

    private SitePropertiesService sitePropertiesService;

    /**
     * If not null the value overrides setting in site properties.
     */
    private Boolean overridingSitePropertyCreateUrlAsPath;

    /**
     * Whether to encode & as &amp; or not.
     */
    private boolean htmlEscapeParameterAmps = false;


    public String createFullPathForRedirect( HttpServletRequest request, SiteKey siteKey, String path )
    {
        if ( siteIsCreatingRelativeUrlsFromRoot( siteKey ) )
        {
            String pathFromRoot;

            //if path is local or missing vhost - correct path
            if ( VirtualHostHelper.hasBasePath( request ) && !path.startsWith( VirtualHostHelper.getBasePath( request ) ) )
            {
                if ( path.startsWith( "/" ) )
                {
                    pathFromRoot = VirtualHostHelper.getBasePath( request ) + path;
                }
                else
                {
                    pathFromRoot = VirtualHostHelper.getBasePath( request ) + "/" + path;
                }
            }
            else
            {
                pathFromRoot = path;
            }
            return doCreateFullPathForRedirectFromRootPath( siteKey, pathFromRoot );
        }
        else
        {
            return doCreateFullPathForRedirectFromLocalPath( request, siteKey, path );
        }
    }

    public String createPathWithinContextPath( HttpServletRequest request, SitePath sitePath, boolean externalPath )
    {
        String localPathPrefix = resolveLocalSitePathPrefix( request, sitePath.getSiteKey(), externalPath );
        return buildPath( localPathPrefix, sitePath );
    }

    public String createUrl( HttpServletRequest request, SitePath sitePath, boolean includeParamsInPath )
    {

        String basePathOverride = (String) request.getAttribute( Attribute.BASEPATH_OVERRIDE_ATTRIBUTE_NAME );

        return doCreateUrl( request, sitePath, includeParamsInPath, basePathOverride );
    }

    public String createUrlWithBasePathOverride( HttpServletRequest request, SitePath sitePath, boolean includeParamsInPath,
                                                 String basePathOverride )
    {
        return doCreateUrl( request, sitePath, includeParamsInPath, basePathOverride );
    }

    private String doCreateUrl( HttpServletRequest request, SitePath sitePath, boolean includeParamsInPath, String basePathOverride )
    {
        boolean createPathOnly;
        if ( overridingSitePropertyCreateUrlAsPath != null )
        {
            createPathOnly = overridingSitePropertyCreateUrlAsPath;
        }
        else
        {
            //check property
            createPathOnly = sitePropertiesService.getSiteProperties( sitePath.getSiteKey() ).getPropertyAsBoolean(
                SitePropertyNames.CREATE_URL_AS_PATH_PROPERTY );
        }

        SiteBasePath siteBasePath = SiteBasePathResolver.resolveSiteBasePath( request, sitePath.getSiteKey() );
        SiteBasePathAndSitePath siteBasePathAndSitePath = new SiteBasePathAndSitePath( siteBasePath, sitePath );

        String url;

        if ( basePathOverride != null )
        {
            url = basePathOverride;
            String siteLocalUrl = sitePathAndSitePathToString( sitePath.getSiteKey(), sitePath.getPathAndParams(), includeParamsInPath );
            if ( siteLocalUrl.startsWith( "/" ) && url.endsWith( "/" ) )
            {
                // preventing double slashes (example: //news/politics)
                siteLocalUrl = siteLocalUrl.substring( 1 );
            }

            url += siteLocalUrl;
        }
        else if ( createPathOnly )
        {
            url = siteBasePathAndSitePathToString( siteBasePathAndSitePath, includeParamsInPath );
        }
        else
        {
            url = createAbsoluteUrl( request, siteBasePathAndSitePath, includeParamsInPath );
        }

        return url;
    }


    public String createAbsoluteUrl( HttpServletRequest request, SiteBasePathAndSitePath siteBasePathAndSitePath,
                                     boolean includeParamsInPath )
    {
        String pathFromRoot = siteBasePathAndSitePathToString( siteBasePathAndSitePath, includeParamsInPath );
        return createAbsoluteUrl( request, pathFromRoot );
    }

    private String createAbsoluteUrl( HttpServletRequest request, String pathFromRoot )
    {
        URL url;
        try
        {
            String scheme = HttpServletUtil.getScheme( request );
            final int port = request.getServerPort();

            if ( "http".equalsIgnoreCase( scheme ) && ( port == DEFAULT_HTTP_PORT ) )
            {
                url = new URL( scheme, request.getServerName(), pathFromRoot );
            }
            else if ( "https".equalsIgnoreCase( scheme ) && ( port == DEFAULT_HTTPS_PORT ) )
            {
                url = new URL( scheme, request.getServerName(), pathFromRoot );
            }
            else
            {
                url = new URL( scheme, request.getServerName(), port, pathFromRoot );
            }
        }
        catch ( MalformedURLException e )
        {
            throw new RuntimeException( "Failed to create absolute url to path: " + pathFromRoot, e );
        }

        return url.toString();
    }

    private String siteBasePathAndSitePathToString( SiteBasePathAndSitePath siteBasePathAndSitePath, boolean includeParamsInPath )
    {
        SiteBasePathAndSitePathToStringBuilder siteBasePathAndSitePathToStringBuilder = new SiteBasePathAndSitePathToStringBuilder();
        siteBasePathAndSitePathToStringBuilder.setEncoding( characterEncoding );
        siteBasePathAndSitePathToStringBuilder.setHtmlEscapeParameterAmps( htmlEscapeParameterAmps );
        siteBasePathAndSitePathToStringBuilder.setIncludeFragment( true );
        siteBasePathAndSitePathToStringBuilder.setIncludeParamsInPath( includeParamsInPath );
        siteBasePathAndSitePathToStringBuilder.setUrlEncodePath( true );

        return siteBasePathAndSitePathToStringBuilder.toString( siteBasePathAndSitePath );
    }

    private String sitePathAndSitePathToString( SiteKey siteKey, PathAndParams siteLocalPathAndParams, boolean includeParamsInPath )
    {
        PathAndParamsToStringBuilder pathAndParamsToStringBuilder = new PathAndParamsToStringBuilder();
        pathAndParamsToStringBuilder.setEncoding( characterEncoding );
        pathAndParamsToStringBuilder.setHtmlEscapeParameterAmps( htmlEscapeParameterAmps );
        pathAndParamsToStringBuilder.setIncludeFragment( true );
        pathAndParamsToStringBuilder.setIncludeParamsInPath( includeParamsInPath );
        pathAndParamsToStringBuilder.setUrlEncodePath( true );

        return pathAndParamsToStringBuilder.toString( siteLocalPathAndParams );
    }

    /**
     * This method must behave different under redirect and forward in cases where rewrite is active.
     */
    private String resolveLocalSitePathPrefix( HttpServletRequest request, SiteKey siteKey, boolean externalPath )
    {
        if ( externalPath && VirtualHostHelper.hasBasePath( request ) )
        {
            return VirtualHostHelper.getBasePath( request );
        }
        else
        {
            return DEFAULT_SITEPATH_PREFIX + "/" + siteKey;
        }
    }

    /**
     * Builds up a path from given localPathPrefix and localPath in sitePath. Ensures that the localPath gets url
     * encoded.
     */
    private String buildPath( String localPathPrefix, SitePath sitePath )
    {
        final String localPathEncoded = sitePath.getLocalPath().getAsUrlEncoded( true, characterEncoding );
        return localPathPrefix + localPathEncoded;
    }

    private String doCreateFullPathForRedirectFromRootPath( SiteKey siteKey, String pathFromRoot )
    {
        final StringBuilder stringBuilder = new StringBuilder();

        if ( !pathFromRoot.startsWith( "/" ) && !pathFromRoot.equals( "" ) )
        {
            pathFromRoot = "/" + pathFromRoot;
        }

        stringBuilder.append( encodePath( pathFromRoot, siteKey ) );

        return stringBuilder.toString();
    }

    /**
     * Builds up a path consisting of contextPath and sitePath from given params. Ensures that the localPath gets url
     * encoded.
     */
    private String doCreateFullPathForRedirectFromLocalPath( HttpServletRequest request, SiteKey siteKey, String localPath )
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append( request.getContextPath() );
        stringBuilder.append( resolveLocalSitePathPrefix( request, siteKey, true ) );

        if ( !localPath.startsWith( "/" ) && !localPath.equals( "" ) )
        {
            localPath = "/" + localPath;
        }

        stringBuilder.append( encodePath( localPath, siteKey ) );

        return stringBuilder.toString();
    }

    private String encodePath( String path, SiteKey siteKey )
    {
        return UrlPathEncoder.encodeUrlPath( path, characterEncoding );
    }

    private boolean siteIsCreatingRelativeUrlsFromRoot( SiteKey siteKey )
    {
        return sitePropertiesService.getSiteProperties( siteKey ).getPropertyAsBoolean( SitePropertyNames.CREATE_URL_AS_PATH_PROPERTY );
    }

    public void setOverridingSitePropertyCreateUrlAsPath( final Boolean value )
    {
        if ( value != null )
        {
            this.overridingSitePropertyCreateUrlAsPath = value;
        }
    }

    public void setHtmlEscapeParameterAmps( boolean htmlEscapeParameterAmps )
    {
        this.htmlEscapeParameterAmps = htmlEscapeParameterAmps;
    }

    @Value("${cms.url.characterEncoding}")
    public void setCharacterEncoding( String characterEncoding )
    {
        this.characterEncoding = characterEncoding;
    }

    public String getCharacterEncoding()
    {
        return characterEncoding;
    }

    @Autowired
    public void setSitePropertiesService( SitePropertiesService value )
    {
        this.sitePropertiesService = value;
    }
}
