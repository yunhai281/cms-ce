/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.captcha;

import javax.servlet.http.HttpSession;

/**
 * Repository for the sentral captcha service.
 */
public interface CaptchaRepository
{
    CaptchaInfo createCaptcha( HttpSession session );

    boolean validateCaptcha( HttpSession session, String userResponse );
}
