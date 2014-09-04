/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.api.client.model.content.image;

import java.io.Serializable;

import com.enonic.cms.api.client.model.content.TextInput;

public class ImagePhotographerNameInput
    extends TextInput
    implements Serializable
{
    private static final long serialVersionUID = -3214832155002963640L;

    public ImagePhotographerNameInput( String value )
    {
        super( "photographer-name", value );
    }
}
