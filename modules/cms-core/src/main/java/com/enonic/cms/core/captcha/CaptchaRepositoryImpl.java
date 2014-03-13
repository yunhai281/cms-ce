/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.captcha;

import java.awt.Color;
import java.awt.Font;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;

import nl.captcha.Captcha;
import nl.captcha.backgrounds.TransparentBackgroundProducer;
import nl.captcha.gimpy.RippleGimpyRenderer;
import nl.captcha.text.producer.DefaultTextProducer;

@Component("captchaRepository")
public final class CaptchaRepositoryImpl
    implements CaptchaRepository
{
    private final static String CAPTCHA_OBJECT = "__captcha__";

    private final static int FONT_SIZE = 24;

    private final static int IMAGE_WIDTH = 160;

    private final static int IMAGE_HEIGHT = 50;

    private final static int WORD_LENGTH = 5;

    private final static char[] ALLOWED_CHARS =
        {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '2', '3',
            '4', '5', '6', '7', '8', '9'};

    private final static List<Color> COLOR_LIST = ImmutableList.of( Color.BLACK );

    private final static List<Font> FONT_LIST =
        ImmutableList.of( new Font( "Dialog", Font.BOLD, FONT_SIZE ), new Font( "Serif", Font.BOLD, FONT_SIZE ),
                          new Font( "SansSerif", Font.BOLD, FONT_SIZE ) );

    @Override
    public CaptchaInfo createCaptcha( final HttpSession session )
    {
        final Captcha captcha = createCaptchaObject();
        session.setAttribute( CAPTCHA_OBJECT, captcha );
        return new CaptchaInfoImpl( captcha );
    }

    @Override
    public boolean validateCaptcha( final HttpSession session, final String userResponse )
    {
        final Captcha answer = getCaptchaFromSession( session );
        return ( answer != null ) && answer.isCorrect( userResponse );
    }

    private Captcha getCaptchaFromSession( final HttpSession session )
    {
        final Object captcha = session.getAttribute( CAPTCHA_OBJECT );
        if ( captcha instanceof Captcha )
        {
            session.removeAttribute( CAPTCHA_OBJECT );
            return (Captcha) captcha;
        }

        return null;
    }

    private Captcha createCaptchaObject()
    {
        final Captcha.Builder builder = new Captcha.Builder( IMAGE_WIDTH, IMAGE_HEIGHT );
        builder.addBackground( new TransparentBackgroundProducer() );
        builder.gimp( new RippleGimpyRenderer() );

        final DefaultTextProducer textProducer = new DefaultTextProducer( WORD_LENGTH, ALLOWED_CHARS );
        final DefaultWordRenderer wordRenderer = new DefaultWordRenderer( COLOR_LIST, FONT_LIST );
        builder.addText( textProducer, wordRenderer );

        return builder.build();
    }
}

