/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.vertical.engine;

public final class MenuItemAccessRight
    extends AccessRight
{
    private boolean publish = false;

    private boolean administrate = false;

    private boolean create = false;

    public MenuItemAccessRight()
    {
    }

    public void setPublish( final boolean publish )
    {
        this.publish = publish;
    }

    public void setAdministrate( final boolean administrate )
    {
        this.administrate = administrate;
    }

    public void setCreate( final boolean create )
    {
        this.create = create;
    }

    public boolean getPublish()
    {
        return publish;
    }

    public boolean getAdministrate()
    {
        return administrate;
    }

    public boolean getCreate()
    {
        return create;
    }
}
