/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.upgrade.task;

import com.enonic.cms.upgrade.UpgradeContext;

final class UpgradeModel0213
    extends AbstractUpgradeTask
{
    public UpgradeModel0213()
    {
        super( 213 );
    }

    public void upgrade( final UpgradeContext context )
        throws Exception
    {
        context.logInfo( "Adding index to the column cov_cov_lSnapshotSource" );

        context.logInfo( "Drop all current constraints on table 'tContentVersion'" );
        context.dropTableConstraints( "tContentVersion", true );

        context.logInfo( "Re-create all constraints on table 'tContentVersion', including new index on cov_cov_lSnapshotSource" );
        context.createTableConstraints( "tContentVersion", true );
    }
}
