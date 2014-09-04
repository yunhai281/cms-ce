/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.api.client.model.content.image;

import java.io.Serializable;

import com.enonic.cms.api.client.model.content.TextInput;

public class ImageCopyrightInput
    extends TextInput
    implements Serializable
{

    private static final long serialVersionUID = 6618848419877945964L;

    public ImageCopyrightInput( String value )
    {
        super( "copyright", value );
    }
}
