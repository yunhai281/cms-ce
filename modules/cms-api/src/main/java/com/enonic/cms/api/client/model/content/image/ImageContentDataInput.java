/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.api.client.model.content.image;

import java.io.Serializable;

public class ImageContentDataInput
    implements Serializable
{
    private static final long serialVersionUID = -3954348979920687656L;

    /**
     * The name of the image, normally the filename.
     */
    public ImageNameInput name;

    /**
     * A general description of the image.
     */
    public ImageDescriptionInput description;

    /**
     * The name of the photographer.
     */
    public ImagePhotographerNameInput photographerName;

    /**
     * The e-mail of the photographer.
     */
    public ImagePhotographerEMailInput photographerEMail;

    /**
     * Copyright information for the image.
     */
    public ImageCopyrightInput copyright;

    /**
     * A set of keywords that can be used to look up the image.
     */
    public ImageKeywordsInput keywords;

    /**
     * The image itself.
     */
    public ImageBinaryInput binary;

}
