package com.enonic.cms.core.captcha;

import java.awt.image.BufferedImage;

import nl.captcha.Captcha;

final class CaptchaInfoImpl
    implements CaptchaInfo
{
    private final Captcha captcha;

    public CaptchaInfoImpl( final Captcha captcha )
    {
        this.captcha = captcha;
    }

    @Override
    public String getAnswer()
    {
        return this.captcha.getAnswer();
    }

    @Override
    public BufferedImage getImage()
    {
        return this.captcha.getImage();
    }
}
