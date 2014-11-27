/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.security.group;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Jul 20, 2009
 */
public class GroupKeyComparator
    implements Comparator<GroupKey>, Serializable
{
    private static final long serialVersionUID = -4975005262376537140L;

    public int compare( GroupKey a, GroupKey b )
    {
        return a.toString().compareTo( b.toString() );
    }
}
