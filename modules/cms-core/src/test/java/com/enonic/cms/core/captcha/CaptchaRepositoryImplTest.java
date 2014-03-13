/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.captcha;

import java.awt.image.BufferedImage;

import javax.servlet.http.HttpSession;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.Assert.*;

public class CaptchaRepositoryImplTest
{
    private HttpSession session;

    private CaptchaRepository repository;

    @Before
    public void setup()
    {
        this.session = new MockHttpSession();
        this.repository = new CaptchaRepositoryImpl();
    }

    @Test
    public void testCreateCaptcha()
    {
        final CaptchaInfo info = this.repository.createCaptcha( this.session );
        assertNotNull( info );
        assertNotNull( info.getAnswer() );

        final BufferedImage image = info.getImage();
        assertNotNull( image );

        assertEquals( 160, image.getWidth() );
        assertEquals( 50, image.getHeight() );
    }

    @Test
    public void testValidateCaptcha()
    {
        final CaptchaInfo info = this.repository.createCaptcha( this.session );

        assertTrue( this.repository.validateCaptcha( this.session, info.getAnswer() ) );
        assertFalse( this.repository.validateCaptcha( this.session, info.getAnswer() ) );
    }
}
