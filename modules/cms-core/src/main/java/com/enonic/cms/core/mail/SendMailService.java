/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.mail;

import java.io.IOException;

import javax.mail.MessagingException;

import com.enonic.cms.core.security.user.QualifiedUsername;

public interface SendMailService
{
    void sendMail( AbstractMailTemplate mailTemplate ) throws IOException, MessagingException;

    void sendChangePasswordMail( QualifiedUsername userName, String newPassword );

    void sendChangePasswordMail( QualifiedUsername userName, String newPassword, MessageSettings settings );
}
