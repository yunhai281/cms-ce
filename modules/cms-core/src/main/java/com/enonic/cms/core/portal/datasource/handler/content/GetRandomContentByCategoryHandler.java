package com.enonic.cms.core.portal.datasource.handler.content;

import org.apache.commons.lang.ArrayUtils;
import org.jdom.Document;
import org.springframework.stereotype.Component;

import com.enonic.cms.framework.xml.XMLDocument;

import com.enonic.cms.core.portal.datasource.handler.base.SimpleDataSourceHandler;
import com.enonic.cms.core.portal.datasource.handler.DataSourceRequest;

@Component("ds.GetRandomContentByCategoryHandler")
public final class GetRandomContentByCategoryHandler
    extends SimpleDataSourceHandler
{
    public GetRandomContentByCategoryHandler()
    {
        super( "getRandomContentByCategory" );
    }

    @Override
    public Document handle( final DataSourceRequest req )
        throws Exception
    {
        final Integer[] keys = param( req, "categoryKeys" ).required().asIntegerArray();
        int[] categoryKeys = ArrayUtils.toPrimitive( keys );
        final int levels = param( req, "levels" ).asInteger( 1 );
        final String query = param( req, "query" ).asString( "" );
        final int count = param( req, "count" ).asInteger( 10 );
        final boolean includeData = param( req, "includeData" ).asBoolean( true );
        final int childrenLevel = param( req, "childrenLevel" ).asInteger( 1 );
        final int parentLevel = param( req, "parentLevel" ).asInteger( 0 );

        XMLDocument document =
            dataSourceService.getRandomContentByCategory( req, categoryKeys, levels, query, count, includeData, childrenLevel, parentLevel );
        return document.getAsJDOMDocument();
    }
}
