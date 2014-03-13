/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.captcha;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.jdom.Document;
import org.jdom.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.enonic.esl.containers.ExtendedMap;

import com.enonic.cms.framework.xml.XMLDocument;
import com.enonic.cms.framework.xml.XMLDocumentFactory;

import com.enonic.cms.core.Attribute;
import com.enonic.cms.core.security.SecurityService;
import com.enonic.cms.core.structure.SiteKey;
import com.enonic.cms.core.structure.SitePath;
import com.enonic.cms.core.structure.SitePropertiesService;
import com.enonic.cms.core.structure.SitePropertyNames;

@Component("captchaService")
public class CaptchaServiceImpl
    implements CaptchaService
{
    @Autowired
    private SecurityService securityService;

    @Autowired
    private SitePropertiesService sitePropertiesService;

    @Autowired
    private CaptchaRepository captchaRepository;

    /**
     * @inheritDoc
     */
    public Boolean validateCaptcha( ExtendedMap formItems, HttpServletRequest request, String handler, String operation )
    {
        SitePath originalSitePath = (SitePath) request.getAttribute( Attribute.ORIGINAL_SITEPATH );
        Object captchaResponse = getCaptchaResponse( formItems );
        boolean forceCaptcha = hasCaptchaCheck( originalSitePath.getSiteKey(), handler, operation );

        if ( forceCaptcha || ( captchaResponse != null ) )
        {
            return validateInput( request.getSession( true ), captchaResponse );
        }
        else
        {
            return null;
        }
    }

    /**
     * @inheritDoc
     */
    public boolean hasCaptchaCheck( SiteKey siteKey, String handler, String operation )
    {
        if ( !securityService.getLoggedInPortalUser().isAnonymous() )
        {
            return false;
        }
        String sitePropertyVariable = SitePropertyNames.SITE_PROPERTY_CAPTCHA_ENABLE.getKeyName() + "." + handler;
        String sitePropertySetting = sitePropertiesService.getSiteProperties( siteKey ).getProperty( sitePropertyVariable );
        if ( sitePropertySetting == null )
        {
            return false;
        }
        else
        {
            sitePropertySetting = sitePropertySetting.trim();
        }
        return sitePropertySetting.equals( "*" ) || sitePropertySetting.equals( operation );
    }

    private boolean validateInput( HttpSession session, Object captchaResponse )
    {
        return ( captchaResponse != null ) && this.captchaRepository.validateCaptcha( session, captchaResponse.toString() );
    }

    private Object getCaptchaResponse( ExtendedMap formItems )
    {
        return formItems.get( FORM_VARIABLE_CAPTCHA_RESPONSE, null );
    }

    /**
     * @inheritDoc
     */
    public XMLDocument buildErrorXMLForSessionContext( ExtendedMap formItems )
    {
        Element root = new Element( "form" );
        Document doc = new Document( root );
        for ( Object key : formItems.keySet() )
        {
            final String name = key.toString();
            if ( !( ( name.charAt( 0 ) == '_' ) && ( name.charAt( 1 ) != '_' ) ) )
            {
                Object value = formItems.get( name );
                if ( value instanceof String[] )
                {
                    for ( String arrayValue : (String[]) value )
                    {
                        root.addContent( new Element( "parameter" ).setAttribute( "name", name ).setText( arrayValue ) );
                    }
                }
                else
                {
                    root.addContent( new Element( "parameter" ).setAttribute( "name", name ).setText( String.valueOf( value ) ) );
                }
            }
        }
        return XMLDocumentFactory.create( doc );
    }

    public void setSitePropertiesService( SitePropertiesService service )
    {
        sitePropertiesService = service;
    }

    public void setSecurityService( SecurityService service )
    {
        securityService = service;
    }

    public void setCaptchaRepository( CaptchaRepository repo )
    {
        captchaRepository = repo;
    }
}
