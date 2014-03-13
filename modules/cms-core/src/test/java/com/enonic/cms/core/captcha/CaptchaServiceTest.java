/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.captcha;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

import com.enonic.esl.containers.ExtendedMap;

import com.enonic.cms.framework.xml.XMLDocument;

import com.enonic.cms.core.Attribute;
import com.enonic.cms.core.security.SecurityService;
import com.enonic.cms.core.security.user.User;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.security.user.UserType;
import com.enonic.cms.core.structure.SiteKey;
import com.enonic.cms.core.structure.SitePath;
import com.enonic.cms.core.structure.SiteProperties;
import com.enonic.cms.core.structure.SitePropertiesService;

import static org.junit.Assert.*;

public class CaptchaServiceTest
{
    private SecurityService securityService;

    private MockHttpServletRequest req;

    private ExtendedMap formItems;

    private SitePropertiesService sitePropsService;

    private CaptchaRepository captchaRepository;

    private CaptchaServiceImpl captchaService;

    @Before
    public void setUp()
    {
        this.req = new MockHttpServletRequest();
        this.req.setAttribute( Attribute.ORIGINAL_SITEPATH, new SitePath( new SiteKey( 0 ), "/userServices/content/create" ) );
        this.req.setSession( new MockHttpSession( null, "13" ) );

        this.formItems = new ExtendedMap();
        this.formItems.put( "abc", "123" );
        this.formItems.put( "def", "456" );
        this.formItems.put( "_ghi", "789" );
        this.formItems.put( "_jkl", new String[]{"EVS", "xsl", "xml", "cms"} );
        this.formItems.put( "mno", new String[]{"Enonic", "Vertical Site", "Bring", "NAV", "Statens Vegvesen", "Norsk Tipping"} );

        this.sitePropsService = Mockito.mock( SitePropertiesService.class );
        this.securityService = Mockito.mock( SecurityService.class );
        this.captchaRepository = new CaptchaRepositoryImpl();

        this.captchaService = new CaptchaServiceImpl();
        this.captchaService.setSecurityService( this.securityService );
        this.captchaService.setSitePropertiesService( this.sitePropsService );
        this.captchaService.setCaptchaRepository( this.captchaRepository );
    }

    private void setupProperties( final SiteKey siteKey, final Properties props )
    {
        Mockito.when( this.sitePropsService.getSiteProperties( siteKey ) ).thenReturn( new SiteProperties( siteKey, props ) );
    }

    private void setupLoggedInUser( final User user )
    {
        Mockito.when( this.securityService.getLoggedInPortalUser() ).thenReturn( user );
    }

    private void setupCapchaResponse()
    {
        final CaptchaInfo info = this.captchaRepository.createCaptcha( this.req.getSession() );
        this.formItems.put( CaptchaServiceImpl.FORM_VARIABLE_CAPTCHA_RESPONSE, info.getAnswer() );
    }

    @Test
    public void testHandleNonCaptchaProtectedServices()
    {
        final Properties props = new Properties();

        setupProperties( new SiteKey( 0 ), props );
        setupLoggedInUser( createUser( "anonymous", UserType.ANONYMOUS ) );

        this.req.setAttribute( Attribute.ORIGINAL_SITEPATH, new SitePath( new SiteKey( 0 ), "/_services/dog/create" ) );
        assertNull( this.captchaService.validateCaptcha( this.formItems, this.req, "dog", "create" ) );

        this.req.setAttribute( Attribute.ORIGINAL_SITEPATH, new SitePath( new SiteKey( 0 ), "/_services/cat/create" ) );
        assertNull( this.captchaService.validateCaptcha( this.formItems, this.req, "cat", "create" ) );
    }

    @Test
    public void testHandleCaptchaAsAdmin()
    {
        setupLoggedInUser( createUser( "admin", UserType.ADMINISTRATOR ) );

        assertNull( this.captchaService.validateCaptcha( this.formItems, this.req, "content", "create" ) );
        assertNull( this.captchaService.validateCaptcha( this.formItems, this.req, "content", "create" ) );
    }

    @Test
    public void testHandleCaptchaAsAnonymous()
    {
        final Properties props = new Properties();
        props.setProperty( "cms.site.httpServices.captchaEnabled.form", "*" );

        setupProperties( new SiteKey( 0 ), props );
        setupLoggedInUser( createUser( "anonymous", UserType.ANONYMOUS ) );

        setupCapchaResponse();
        assertTrue( this.captchaService.validateCaptcha( this.formItems, this.req, "form", "create" ) );
        assertFalse( this.captchaService.validateCaptcha( this.formItems, this.req, "form", "create" ) );

        final XMLDocument xmlDoc = this.captchaService.buildErrorXMLForSessionContext( this.formItems );
        assertNotNull( xmlDoc );

        final String xml = xmlDoc.getAsString();
        assertTrue( xml.contains( "123" ) );
        assertTrue( xml.contains( "def" ) );
        assertFalse( xml.contains( "ghi" ) );
        assertFalse( xml.contains( "EVS" ) );
        assertTrue( xml.contains( "Norsk Tipping" ) );
    }

    @Test
    public void testHasCaptchaCheck()
    {
        final Properties props = new Properties();
        props.setProperty( "cms.site.httpServices.captchaEnabled.form", "update" );
        props.setProperty( "cms.site.httpServices.captchaEnabled.content", "*" );

        setupProperties( new SiteKey( 0 ), props );
        setupLoggedInUser( createUser( "anonymous", UserType.ANONYMOUS ) );

        assertFalse( this.captchaService.hasCaptchaCheck( new SiteKey( 0 ), "form", "create" ) );
        assertTrue( this.captchaService.hasCaptchaCheck( new SiteKey( 0 ), "content", "update" ) );
        assertTrue( this.captchaService.hasCaptchaCheck( new SiteKey( 0 ), "form", "update" ) );
    }

    private UserEntity createUser( String name, UserType type )
    {
        UserEntity user = new UserEntity();
        user.setName( name );
        user.setType( type );
        return user;
    }
}
