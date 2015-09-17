/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal.rendering;

import com.enonic.vertical.engine.handlers.MenuHandler;

import com.enonic.cms.core.security.user.UserEntity;
import com.enonic.cms.core.structure.RunAsType;
import com.enonic.cms.core.structure.menuitem.MenuItemEntity;
import com.enonic.cms.core.structure.portlet.PortletEntity;

public class PortletRunAsUserResolver
{

    public static UserEntity resolveRunAsUser( PortletEntity portlet, UserEntity currentUser, MenuItemEntity menuItem,
                                               MenuHandler menuHandler )
    {
        if ( currentUser.isAnonymous() )
        {
            // Anonymous user cannot run as any other user
            return currentUser;
        }

        RunAsType runAs = portlet.getRunAs();

        if ( runAs.equals( RunAsType.PERSONALIZED ) )
        {
            return currentUser;
        }
        else if ( runAs.equals( RunAsType.DEFAULT_USER ) )
        {
            UserEntity defaultRunAsUser = menuHandler.getRunAsUserForSite( portlet.getSite().getKey() );
            if ( defaultRunAsUser != null )
            {
                return defaultRunAsUser;
            }
            return null;
        }
        else if ( runAs.equals( RunAsType.INHERIT ) )
        {
            return inherit( currentUser, menuItem, menuHandler );
        }
        else
        {
            throw new IllegalArgumentException( "Unsupported runAs: " + runAs );
        }
    }

    private static UserEntity inherit( UserEntity current, MenuItemEntity menuItem, MenuHandler menuHandler )
    {
        if ( menuItem != null )
        {
            return menuItem.resolveRunAsUser( current, false, menuHandler );
        }
        else
        {
            throw new IllegalStateException( "Expected to render portlet in context of either a menuitem or a page template" );
        }
    }
}
