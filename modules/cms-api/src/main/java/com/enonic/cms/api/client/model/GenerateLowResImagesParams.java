package com.enonic.cms.api.client.model;

public class GenerateLowResImagesParams
    extends AbstractParams
{
    private static final long serialVersionUID = 7522783005453240374L;

    /**
     * List categories in which all images should be (re)generated, or NULL to (re)generate low res images of all images in entire archive.
     * If a lot of images will be regenerated at the same time, the transaction timeout should be temporarily increased while executing the
     * method.  Regenerating one image may take up to or more than 2 seconds, depending on the speed of the hardware used.
     */
    public Integer[] categoryKeys;

    /**
     * Set to specific size, or NULL, if all sizes should be (re)genenerated.
     */
    public StandardImageSize imageSize;
}
