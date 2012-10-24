package com.enonic.cms.upgrade.task.datasource.method;

import org.jdom.Element;

final class GetTimeZonesConverter
    extends DataSourceMethodConverter
{
    public GetTimeZonesConverter()
    {
        super( "getTimeZones" );
    }

    @Override
    public Element convert( final String[] params )
    {
        if ( params.length > 0 )
        {
            return null;
        }

        return method().build();
    }
}
