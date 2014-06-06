/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal;

import org.junit.Test;

import com.enonic.cms.core.InvalidKeyException;
import com.enonic.cms.core.structure.SiteKey;
import com.enonic.cms.core.structure.menuitem.MenuItemKey;
import com.enonic.cms.core.structure.portlet.PortletKey;

import static org.junit.Assert.*;

public class PortalInstanceKeyResolverTest
{
    private PortalInstanceKeyResolver resolver = new PortalInstanceKeyResolver();

    private SiteKey contextSiteKey = new SiteKey( 11 );

    @Test
    public void testPortalInstanceKeyResolver()
    {

        PortalInstanceKey key1 = resolver.resolvePortalInstanceKey( "WINDOW:43:191", contextSiteKey );
        PortalInstanceKey key2 = resolver.resolvePortalInstanceKey( "PAGE:812", contextSiteKey );
        PortalInstanceKey key4 = resolver.resolvePortalInstanceKey( "SITE:11", contextSiteKey );

        assertEquals( new MenuItemKey( 43 ), key1.getMenuItemKey() );
        assertEquals( new PortletKey( 191 ), key1.getPortletKey() );
        assertNotNull( key1.getSiteKey() );

        assertEquals( new MenuItemKey( 812 ), key2.getMenuItemKey() );
        assertNull( key2.getPortletKey() );
        assertNotNull( key2.getSiteKey() );

        assertNull( key4.getMenuItemKey() );
        assertNull( key4.getPortletKey() );
        assertEquals( new SiteKey( 11 ), key4.getSiteKey() );
    }

    @Test
    public void testPortalInstanceKeyResolverErrors()
    {
        try
        {
            resolver.resolvePortalInstanceKey( null, contextSiteKey );
            fail( "null, is not a valid instance key." );
        }
        catch ( IllegalArgumentException e )
        {
            assertEquals( "No instanceKey provided, input is empty.", e.getMessage() );
        }

        try
        {
            resolver.resolvePortalInstanceKey( "", contextSiteKey );
            fail( "'', is not a valid instance key." );
        }
        catch ( IllegalArgumentException e )
        {
            assertEquals( "No instanceKey provided, input is empty.", e.getMessage() );
        }

        try
        {
            resolver.resolvePortalInstanceKey( "WINDOWS:81:81", contextSiteKey );
            fail( "'WINDOWS:81:81', is not a valid instance key." );
        }
        catch ( IllegalArgumentException e )
        {
            assertEquals( "No valid instance key context in key: WINDOWS:81:81", e.getMessage() );
        }

        try
        {
            resolver.resolvePortalInstanceKey( "WINDO:81:81", contextSiteKey );
            fail( "'WINDO:81:81', is not a valid instance key." );
        }
        catch ( IllegalArgumentException e )
        {
            assertEquals( "No valid instance key context in key: WINDO:81:81", e.getMessage() );
        }

        try
        {
            resolver.resolvePortalInstanceKey( "WINDOW:81:81:81", contextSiteKey );
            fail( "'WINDOW:81:81:81', is not a valid instance key." );
        }
        catch ( IllegalArgumentException e )
        {
            assertEquals( "WINDOW instance key has wrong number of keys: 3", e.getMessage() );
        }

        try
        {
            resolver.resolvePortalInstanceKey( "PAGE:1:1", contextSiteKey );
            fail( "'PAGE:1:1', is not a valid instance key." );
        }
        catch ( IllegalArgumentException e )
        {
            assertEquals( "PAGE instance key has wrong number of keys: 2", e.getMessage() );
        }

        try
        {
            resolver.resolvePortalInstanceKey( "SITE:0:1:2:3:4", contextSiteKey );
            fail( "'SITE:0:1:2:3:4', is not a valid instance key." );
        }
        catch ( IllegalArgumentException e )
        {
            assertEquals( "SITE instance key has wrong number of keys: 5", e.getMessage() );
        }

        try
        {
            resolver.resolvePortalInstanceKey( "SITE:a", contextSiteKey );
            fail( "'SITE:a' is not a valid instance key." );
        }
        catch ( InvalidKeyException e )
        {
            // Success!
        }

        try
        {
            resolver.resolvePortalInstanceKey( "WINDOW:-2:14", contextSiteKey );
            fail( "'WINDOW:-2:14' is not a valid instance key." );
        }
        catch ( InvalidKeyException e )
        {
            // Success!
        }

        try
        {
            resolver.resolvePortalInstanceKey( "WINDOW:PAGE:1", contextSiteKey );
            fail( "'WINDOW:PAGE' is not a valid instance key." );
        }
        catch ( InvalidKeyException e )
        {
            // Success!
        }

    }
}
