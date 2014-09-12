/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.security;

import java.util.ArrayList;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.enonic.esl.servlet.http.CookieUtil;

import com.enonic.cms.core.log.LogService;
import com.enonic.cms.core.log.LogType;
import com.enonic.cms.core.log.StoreNewLogEntryCommand;
import com.enonic.cms.core.login.LoginService;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.security.user.UserKey;
import com.enonic.cms.core.structure.SiteContext;
import com.enonic.cms.core.structure.SiteEntity;
import com.enonic.cms.core.structure.SiteKey;
import com.enonic.cms.core.structure.SiteService;

@Service
public class AutoLoginService
{
    private SecurityService securityService;

    private LoginService loginService;

    private LogService logService;

    private SiteService siteService;

    public UserEntity autologinWithRemoteUser( HttpServletRequest request, SiteEntity site )
    {
        UserEntity user = resolveUserFromRequest( request );
        if ( user == null )
        {
            return securityService.getAnonymousUser();
        }
        if ( !user.isAnonymous() )
        {
            PortalSecurityHolder.setLoggedInUser( user.getKey() );
            logLogin( user, request.getRemoteAddr(), site, LogType.AUTO_LOGIN );
        }
        return user;
    }

    /**
     * Checks the cookies to see if a user is allready logged in on the site.
     * The login information in the cookie have to match user data in the database.
     *
     * @param site     The site to check if the user is logged in.
     * @param request  The Http Request, containing the cookies.
     * @param response The Http Response, on which the cookie is cleared, if the user has expired.
     * @return The logged in user, if it exists, otherwise, the anonymous user.
     */
    public UserEntity autologinWithCookie( SiteEntity site, HttpServletRequest request, HttpServletResponse response )
    {
        UserEntity user = resolveUserFromCookie( site.getKey(), request, response );
        if ( user == null )
        {
            return securityService.getAnonymousUser();
        }
        if ( !user.isAnonymous() )
        {
            PortalSecurityHolder.setLoggedInUser( user.getKey() );
            logLogin( user, request.getRemoteAddr(), site, LogType.REMEMBERED_LOGIN );
        }
        return user;
    }

    private UserEntity resolveUserFromCookie( SiteKey siteKey, HttpServletRequest request, HttpServletResponse response )
    {

        String cookieName = "guid-" + siteKey.toInt();
        ArrayList<Cookie> guidCookies = CookieUtil.getCookies( request, cookieName );

        Cookie cookie = null;
        for ( Cookie c : guidCookies )
        {
            if ( c.getValue() != null && !c.getValue().equals( "" ) )
            {
                cookie = c;
                break;
            }
        }

        if ( cookie == null || cookie.getValue() == null )
        {
            return null;
        }

        String cookieGUID = cookie.getValue();

        if ( cookieGUID.length() == 0 )
        {
            cookie.setValue( null );
            response.addCookie( cookie );
            return null;
        }

        UserKey userKey = loginService.getRememberedLogin( cookieGUID, siteKey );
        if ( userKey == null )
        {
            cookie.setValue( null );
            response.addCookie( cookie );
            return null;
        }

        UserEntity userEntity = securityService.getUser( userKey );
        SiteContext siteContext = siteService.getSiteContext( siteKey );

        if ( siteContext.isAuthenticationLoggingEnabled() )
        {
            final StoreNewLogEntryCommand command = new StoreNewLogEntryCommand();
            command.setType( LogType.LOGIN );
            command.setInetAddress( request.getRemoteAddr() );
            command.setTitle( userEntity.getDisplayName() + " (" + userEntity.getName() + ")" );
            command.setXmlData( SecurityLoggingXml.createUserStoreDataDoc( userEntity.getQualifiedName() ) );
            command.setUser( userKey );

            this.logService.storeNew( command );
        }

        return userEntity;
    }

    private UserEntity resolveUserFromRequest( HttpServletRequest request )
    {

        String remoteUserUID = request.getRemoteUser();
        if ( remoteUserUID == null )
        {
            return null;
        }

        return securityService.getUserFromDefaultUserStore( remoteUserUID );
    }

    private void logLogin( final UserEntity user, final String remoteIp, SiteEntity site, LogType loginType )
    {
        final StoreNewLogEntryCommand command = new StoreNewLogEntryCommand();
        command.setType( loginType );
        command.setInetAddress( remoteIp );
        command.setUser( user.getKey() );
        command.setTitle( user.getDisplayName() + " (" + user.getName() + ")" );
        command.setXmlData( SecurityLoggingXml.createUserStoreDataDoc( user.getQualifiedName() ) );
        command.setSite( site );

        this.logService.storeNew( command );
    }


    @Autowired
    public void setSiteService( SiteService siteService )
    {
        this.siteService = siteService;
    }

    @Autowired
    public void setLogService( LogService logService )
    {
        this.logService = logService;
    }

    @Autowired
    public void setLoginService( LoginService loginService )
    {
        this.loginService = loginService;
    }

    @Autowired
    public void setSecurityService( SecurityService value )
    {
        this.securityService = value;
    }

}
