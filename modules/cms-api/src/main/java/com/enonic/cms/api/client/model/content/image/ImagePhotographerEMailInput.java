/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.api.client.model.content.image;

import java.io.Serializable;

import com.enonic.cms.api.client.model.content.TextInput;

public class ImagePhotographerEMailInput
    extends TextInput
    implements Serializable
{
    private static final long serialVersionUID = 1575661703129815012L;

    public ImagePhotographerEMailInput( String value )
    {
        super( "photographer-email", value );
    }
}
