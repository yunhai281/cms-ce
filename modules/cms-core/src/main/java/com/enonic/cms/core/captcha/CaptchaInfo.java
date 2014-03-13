package com.enonic.cms.core.captcha;

import java.awt.image.BufferedImage;

public interface CaptchaInfo
{
    public String getAnswer();

    public BufferedImage getImage();
}
