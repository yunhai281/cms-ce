/*
 * Copyright 2000-2011 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.server.service.mbean.cache;

public class Page
    extends AbstractCache
    implements PageMBean
{
    public Page()
    {
        super( "page" );
    }
}