/*
 * Copyright 2000-2011 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.domain.content.contenttype.dataentryconfig;


public class ImagesDataEntryConfig
    extends AbstractBaseDataEntryConfig
{
    public ImagesDataEntryConfig( String name, boolean required, String displayName, String xpath )
    {
        super( name, required, DataEntryConfigType.IMAGES, displayName, xpath );
    }
}