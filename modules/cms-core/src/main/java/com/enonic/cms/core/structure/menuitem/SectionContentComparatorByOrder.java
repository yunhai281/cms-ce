/*
 * Copyright 2000-2016 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.structure.menuitem;

import java.io.Serializable;
import java.util.Comparator;

import com.enonic.cms.core.structure.menuitem.section.SectionContentEntity;

public class SectionContentComparatorByOrder
    implements Comparator<SectionContentEntity>, Serializable
{
    private static final long serialVersionUID = -5900433713451329377L;

    public int compare( SectionContentEntity a, SectionContentEntity b )
    {
        if (a == b) {
            return 0;
        }

        if ( !a.isApproved() && b.isApproved() )
        {
            return -1;
        }
        else if ( !b.isApproved() && a.isApproved() )
        {
            return 1;
        }
        else if ( !a.isApproved() && !b.isApproved() )
        {
            return keyCompare( a, b );
        }
        else if ( a.getOrder() < b.getOrder() )
        {
            return -1;
        }
        else if ( a.getOrder() == b.getOrder() )
        {
            return keyCompare( a, b );
        }
        else
        {
            return 1;
        }
    }

    private int keyCompare( final SectionContentEntity a, final SectionContentEntity b )
    {
        if ( a.getKey().toInt() < b.getKey().toInt() )
        {
            return -1;
        }
        else if ( a.getKey().toInt() == b.getKey().toInt() )
        {
            return 0;
        }
        else
        {
            return 1;
        }
    }
}
