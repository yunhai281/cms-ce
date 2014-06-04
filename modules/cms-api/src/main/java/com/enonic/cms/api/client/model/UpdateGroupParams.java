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
     * The name of the group.
     */
    public String name;

    /**
     * A description of the group.
     */
    public String description;

    /**
     * Specify if the groups has restricted enrollment or not. Default is true. Only User Store Administrators can add members to restricted
     * groups, but groups that are not restricted can be freely joined by anyone via user services.
     */
    public boolean restricted = true;
}