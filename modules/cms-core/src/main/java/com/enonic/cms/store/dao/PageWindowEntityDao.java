/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.store.dao;

import org.hibernate.Query;
import org.springframework.stereotype.Repository;

import com.enonic.cms.framework.hibernate.support.DeleteBuilder;
import com.enonic.cms.framework.hibernate.support.InClauseBuilder;

import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.structure.page.PageWindowEntity;

@Repository("pageWindowDao")
public final class PageWindowEntityDao
    extends AbstractBaseEntityDao<PageWindowEntity>
    implements PageWindowDao
{
    public PageWindowEntity findByKey( int key )
    {
        return get( PageWindowEntity.class, key );
    }

    public int deleteByPageKeyAndTemplateRegionKey( Integer[] pageKeys, int[] regionKeys )
    {
        final String hql = deleteByPageKeyAndTemplateRegionHQL( pageKeys, regionKeys );

        final Query compiled = getHibernateTemplate().getSessionFactory().getCurrentSession().createQuery( hql );

        compiled.setCacheable( false );
        compiled.setReadOnly( true );

        for (int i = 0; i < pageKeys.length; i++) {
            String parameter = "pageKey" + i;
            compiled.setParameter( parameter, pageKeys[i] );
        }

        for (int i = 0; i < regionKeys.length; i++) {
            String parameter = "regionKey" + i;
            compiled.setParameter( parameter, regionKeys[i] );
        }

        return compiled.executeUpdate();
    }

    private String deleteByPageKeyAndTemplateRegionHQL( Integer[] pageKeys, int[] regionKeys ) {
        final DeleteBuilder hqlQuery = new DeleteBuilder(  );
        hqlQuery.addFromTable( PageWindowEntity.class.getName(), "pwe", DeleteBuilder.NO_JOIN, null );

        hqlQuery.addFilter( "AND", new InClauseBuilder<ContentKey>( "pwe.page.key", ContentKey.convertToList( pageKeys ) )
        {
            public void appendValue( final StringBuffer sql, final ContentKey value )
            {
                sql.append( ":pageKey" ).append( getIndex() );
            }
        }.toString() );

        hqlQuery.addFilter( "AND", new InClauseBuilder<ContentKey>( "pwe.pageTemplateRegion.key", ContentKey.convertToList( regionKeys ) )
        {
            public void appendValue( final StringBuffer sql, final ContentKey value )
            {
                sql.append( ":regionKey" ).append( getIndex() );
            }
        }.toString() );

        return hqlQuery.toString();
    }

    public int deleteByPageKeys( Integer[] pageKeys )
    {
        final String hql = deleteByPageKeysHQL( pageKeys );

        final Query compiled = getHibernateTemplate().getSessionFactory().getCurrentSession().createQuery( hql );

        compiled.setCacheable( false );
        compiled.setReadOnly( true );

        for (int i = 0; i < pageKeys.length; i++) {
            String parameter = "pageKey" + i;
            compiled.setParameter( parameter, pageKeys[i] );
        }

        return compiled.executeUpdate();
    }

    private String deleteByPageKeysHQL( Integer [] pageKeys ) {
        final DeleteBuilder hqlQuery = new DeleteBuilder(  );
        hqlQuery.addFromTable( PageWindowEntity.class.getName(), "pwe", DeleteBuilder.NO_JOIN, null );

        hqlQuery.addFilter( "AND", new InClauseBuilder<ContentKey>( "pwe.page.key", ContentKey.convertToList( pageKeys ) )
        {
            public void appendValue( final StringBuffer sql, final ContentKey value )
            {
                sql.append( ":pageKey" ).append( getIndex() );
            }
        }.toString() );

        return hqlQuery.toString();
    }
}
