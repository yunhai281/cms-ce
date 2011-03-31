/*
 * Copyright 2000-2011 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.domain.content.contenttype.dataentryconfig;


public class ImageDataEntryConfig
    extends AbstractBaseDataEntryConfig
{
    public ImageDataEntryConfig( String name, boolean required, String displayName, String xpath )
    {
        super( name, required, DataEntryConfigType.IMAGE, displayName, xpath );
    }
}