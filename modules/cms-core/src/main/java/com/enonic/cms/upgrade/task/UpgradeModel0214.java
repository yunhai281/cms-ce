package com.enonic.cms.upgrade.task;

import com.enonic.cms.upgrade.UpgradeContext;

public class UpgradeModel0214
    extends AbstractUpgradeTask
{
    public UpgradeModel0214()
    {
        super( 214 );
    }

    public void upgrade( final UpgradeContext context )
        throws Exception
    {
        context.logInfo( "Adding index to the column len_usr_hKey" );

        context.logInfo( "Drop all current constraints on table 'tLogEntry'" );
        context.dropTableConstraints( "tLogEntry", true );

        context.logInfo( "Re-create all constraints on table 'tLogEntry', including new index on len_usr_hKey" );
        context.createTableConstraints( "tLogEntry", true );

    }
}
