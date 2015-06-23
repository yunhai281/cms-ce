/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.store.dao;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

import com.enonic.cms.framework.hibernate.support.InClauseBuilder;
import com.enonic.cms.framework.hibernate.support.SelectBuilder;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.structure.menuitem.section.SectionContentEntity;

class FindContentByKeysQuerier
{

    public static final int EAGER_FETCH_NUMBER_OF_SECTIONS_THRESHOLD = 10;

    public static final int MAXIMUM_NUMBER_OF_PARAMETER_MARKERS = 50;

    private Session hibernateSession;

    private ContentEagerFetches contentEagerFetches;

    private boolean fetchEntitiesAsReadOnly = true;

    FindContentByKeysQuerier( final Session hibernateSession, final ContentEagerFetches contentEagerFetches,
                              final boolean fetchEntitiesAsReadOnly )
    {
        this.hibernateSession = hibernateSession;
        this.contentEagerFetches = contentEagerFetches;
        this.fetchEntitiesAsReadOnly = fetchEntitiesAsReadOnly;
    }

    List<ContentEntity> queryContent( final Collection<ContentKey> contentKeys )
    {
        if ( contentKeys.size() <= MAXIMUM_NUMBER_OF_PARAMETER_MARKERS )
        {
            return queryContentPart( contentKeys );
        }
        else
        {
            List<ContentEntity> result = new ArrayList<ContentEntity>( contentKeys.size() );

            final int numberOfBatches = (contentKeys.size() - 1) / MAXIMUM_NUMBER_OF_PARAMETER_MARKERS + 1;
            ArrayList<List<ContentKey>> batches = new ArrayList<List<ContentKey>>( numberOfBatches );
            for ( int i = 0; i < numberOfBatches; i++ )
            {
                batches.add(new ArrayList<ContentKey>( MAXIMUM_NUMBER_OF_PARAMETER_MARKERS ));
            }

            int i = 0, batchNumber;
            for ( ContentKey key : contentKeys )
            {
                batchNumber = i / MAXIMUM_NUMBER_OF_PARAMETER_MARKERS;
                batches.get( batchNumber ).add( key );
                i++;
            }

            for (List<ContentKey> batch : batches ){
                result.addAll(queryContentPart( batch ));
            }

            return result;
        }
    }

    private List<ContentEntity> queryContentPart( final Collection<ContentKey> contentKeys )
    {
        if (contentKeys.size() == 0) {
            return new ArrayList<ContentEntity>(  );
        }

        final SelectBuilder hqlQuery = new SelectBuilder( 0 );
        hqlQuery.addSelect( "c" );
        hqlQuery.addFromTable( ContentEntity.class.getName(), "c", SelectBuilder.NO_JOIN, null );

        if ( eagerFetchingIsSafe( contentKeys ) )
        {
            if ( contentEagerFetches.hasTable( ContentEagerFetches.Table.ACCESS ) )
            {
                hqlQuery.addFromTable( "c.contentAccessRights", null, SelectBuilder.LEFT_JOIN_FETCH, null );
            }
            if ( contentEagerFetches.hasTable( ContentEagerFetches.Table.MAIN_VERSION ) )
            {
                hqlQuery.addFromTable( "c.mainVersion", null, SelectBuilder.LEFT_JOIN_FETCH, null );
            }
            if ( contentEagerFetches.hasTable( ContentEagerFetches.Table.SECTION_CONTENT ) )
            {
                hqlQuery.addFromTable( "c.sectionContents", null, SelectBuilder.LEFT_JOIN_FETCH, null );
            }
            if ( contentEagerFetches.hasTable( ContentEagerFetches.Table.DIRECT_MENUITEM_PLACEMENT ) )
            {
                hqlQuery.addFromTable( "c.directMenuItemPlacements", null, SelectBuilder.LEFT_JOIN_FETCH, null );
            }
            if ( contentEagerFetches.hasTable( ContentEagerFetches.Table.CONTENT_HOME ) )
            {
                hqlQuery.addFromTable( "c.contentHomes", null, SelectBuilder.LEFT_JOIN_FETCH, null );
            }
        }

        hqlQuery.addFilter( "AND", new InClauseBuilder<ContentKey>( "c.key", contentKeys )
        {
            public void appendValue( StringBuffer sql, ContentKey value )
            {
                sql.append( value.toString() );
            }

        }.toString() );

        final Query compiled = hibernateSession.createQuery( hqlQuery.toString() );
        compiled.setReadOnly( fetchEntitiesAsReadOnly );
        compiled.setCacheable( false );
        //noinspection unchecked
        return compiled.list();
    }

    private boolean eagerFetchingIsSafe( final Collection<ContentKey> contentKeys )
    {
        if ( !contentEagerFetches.hasTable( ContentEagerFetches.Table.SECTION_CONTENT ) )
        {
            return true;
        }

        final SelectBuilder hqlQuery = new SelectBuilder( 0 );
        hqlQuery.addSelect( "count(*)" );
        hqlQuery.addFromTable( SectionContentEntity.class.getName(), "sc", SelectBuilder.NO_JOIN, null );
        hqlQuery.addFilter( "AND", ( new InClauseBuilder<ContentKey>( "sc.content.key", contentKeys )
        {
            @Override
            public void appendValue( StringBuffer sql, ContentKey value )
            {
                sql.append( value.toString() );
            }

        }.toString() ) );

        final Query compiled = hibernateSession.createQuery( hqlQuery.toString() );
        compiled.setCacheable( false );
        compiled.setReadOnly( true );

        int count = ( (Number) compiled.uniqueResult() ).intValue();

        return count <= ( EAGER_FETCH_NUMBER_OF_SECTIONS_THRESHOLD * contentKeys.size() );
    }
}
