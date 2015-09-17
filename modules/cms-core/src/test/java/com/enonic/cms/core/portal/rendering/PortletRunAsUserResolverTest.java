/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal.rendering;

import org.junit.Before;
import org.junit.Test;

import com.enonic.cms.core.security.user.User;
import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.security.user.UserType;
import com.enonic.cms.core.structure.RunAsType;
import com.enonic.cms.core.structure.SiteEntity;
import com.enonic.cms.core.structure.menuitem.MenuItemEntity;
import com.enonic.cms.core.structure.portlet.PortletEntity;

import static org.junit.Assert.*;

/**
 *
 */
public class PortletRunAsUserResolverTest
{

    private SiteEntity site;

    private UserEntity defaultRunAsUser;

    @Before
    public void setUp()
    {
        defaultRunAsUser = createUser( UserType.NORMAL, "virtualBoss" );

        site = new SiteEntity();
    }

    @Test
    public void testResolveRunAsAnonymousUser()
    {
        UserEntity user = createUser( UserType.ANONYMOUS, "anonymous" );
        User runAsUser = PortletRunAsUserResolver.resolveRunAsUser( null, user, null, null );
        assertTrue( "The run as user should be anonymous when input is anonymous", runAsUser.isAnonymous() );
    }

    // After having run green for years, the next four tests have been commented out when a MenuHandler could not be provided.
    public void xtestResolveRunAsUserNoInherit()
    {
        UserEntity loggedInUser = createUser( UserType.NORMAL, "spirrevipp" );

        PortletEntity portlet = createPortlet( site, RunAsType.PERSONALIZED );

        User runAsUser = PortletRunAsUserResolver.resolveRunAsUser( portlet, loggedInUser, null, null );
        assertEquals( "Logged in user is not run as user, despite 'Personalized' run as policy", loggedInUser, runAsUser );

        portlet.setRunAs( RunAsType.DEFAULT_USER );
        runAsUser = PortletRunAsUserResolver.resolveRunAsUser( portlet, loggedInUser, null, null );
        assertEquals( "Run as user is not the default despite 'Default User' run as policy.", defaultRunAsUser, runAsUser );
    }

    public void xtestResolveRunAsUserInheritFromMenu()
    {
        PortletEntity portlet = createPortlet( site, RunAsType.INHERIT );
        UserEntity loggedInUser = createUser( UserType.NORMAL, "spirrevipp" );
        MenuItemEntity menuItem = createMenuItem( site, "Opprøret i Tibet", RunAsType.PERSONALIZED, null );

        User runAsUser = PortletRunAsUserResolver.resolveRunAsUser( portlet, loggedInUser, menuItem, null );
        assertEquals( "Logged in user is not run as user, despite 'Personalized' run as policy", loggedInUser, runAsUser );

        menuItem.setRunAs( RunAsType.DEFAULT_USER );
        runAsUser = PortletRunAsUserResolver.resolveRunAsUser( portlet, loggedInUser, menuItem, null );
        assertEquals( "Run as user is not the default despite 'Default User' run as policy.", defaultRunAsUser, runAsUser );
    }

    public void xtestResolveRunAsUserInheritFromTopLevelMenu()
    {
        PortletEntity portlet = createPortlet( site, RunAsType.INHERIT );
        UserEntity loggedInUser = createUser( UserType.NORMAL, "spirrevipp" );
        MenuItemEntity topLevelMenuItem = createMenuItem( site, "Nyheter", RunAsType.PERSONALIZED, null );
        MenuItemEntity menuItem = createMenuItem( site, "Utenriks", RunAsType.INHERIT, topLevelMenuItem );

        User runAsUser = PortletRunAsUserResolver.resolveRunAsUser( portlet, loggedInUser, menuItem, null );
        assertEquals( "Logged in user is not run as user, despite 'Personalized' run as policy", loggedInUser, runAsUser );

        topLevelMenuItem.setRunAs( RunAsType.DEFAULT_USER );
        runAsUser = PortletRunAsUserResolver.resolveRunAsUser( portlet, loggedInUser, menuItem, null );
        assertEquals( "Run as user is not the default despite 'Default User' run as policy.", defaultRunAsUser, runAsUser );
    }

    public void xtestResolveRunAsUserInheritFromSite()
    {
        PortletEntity portlet = createPortlet( site, RunAsType.INHERIT );
        UserEntity loggedInUser = createUser( UserType.NORMAL, "spirrevipp" );
        MenuItemEntity topLevelMenuItem = createMenuItem( site, "Nyheter", RunAsType.INHERIT, null );
        MenuItemEntity menuItem = createMenuItem( site, "Utenriks", RunAsType.INHERIT, topLevelMenuItem );

        User runAsUser = PortletRunAsUserResolver.resolveRunAsUser( portlet, loggedInUser, menuItem, null );
        assertEquals( "Run as user is not the default despite 'Default User' run as policy.", defaultRunAsUser, runAsUser );
    }

    private UserEntity createUser( UserType type, String uid )
    {
        UserEntity user = new UserEntity();
        user.setDeleted( 0 );
        user.setType( type );
        user.setName( uid );
        return user;
    }

    private PortletEntity createPortlet( SiteEntity site, RunAsType runAsType )
    {
        PortletEntity portlet = new PortletEntity();
        portlet.setSite( site );
        portlet.setRunAs( runAsType );
        return portlet;
    }

    private MenuItemEntity createMenuItem( SiteEntity site, String name, RunAsType runAsType, MenuItemEntity parent )
    {
        MenuItemEntity menuItem = new MenuItemEntity();
        menuItem.setSite( site );
        menuItem.setName( name );
        menuItem.setRunAs( runAsType );
        menuItem.setParent( parent );
        return menuItem;
    }
}
