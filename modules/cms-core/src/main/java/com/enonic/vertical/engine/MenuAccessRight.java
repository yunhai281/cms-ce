/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.vertical.engine;

public final class MenuAccessRight
    extends AccessRight
{
    private boolean create = false;

    private boolean administrate = false;

    public MenuAccessRight()
    {
    }

    public void setCreate( final boolean create )
    {
        this.create = create;
    }

    public void setAdministrate( final boolean administrate )
    {
        this.administrate = administrate;
    }

    public boolean getCreate()
    {
        return create;
    }

    public boolean getAdministrate()
    {
        return administrate;
    }
}
