/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.api.client.model;

import java.io.Serializable;

public class UpdateGroupParams
    extends AbstractParams
    implements Serializable
{

    private static final long serialVersionUID = -1;

    /**
     * Specify group either by qualified group name (&lt;userStoreKey&gt;:&lt;group name&gt;) or key. When specifying a key, prefix with a
     * hash (group = #xxx).
     */
    public String group;

    /**
     * Specify the new name of the group.
     */
    public String name;

    /**
     * Specify the new description of the group.
     */
    public String description;

    /**
     * Specify if the groups is to restricted or not. Default is true. A restricted group is a group that cannot be joined or left via user
     * services.
     */
    public boolean restricted = true;
}