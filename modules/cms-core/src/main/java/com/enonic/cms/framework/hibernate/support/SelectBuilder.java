/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.framework.hibernate.support;

/**
 * Builds Select statements using HQL syntax.   This class can not be used for regular SQL.
 */
public class SelectBuilder extends QueryBuilder
{

    public SelectBuilder( int tabs )
    {
        this.hql = new StringBuffer();
        this.tabs = tabs;
    }

    public SelectBuilder( StringBuffer hql, int tabs )
    {
        this.hql = hql;
        this.tabs = tabs;
    }

    public void addSelect( String columns )
    {
        appendTabs();
        hql.append( "SELECT " ).append( columns );
    }

    public void addSelectColumn( String column )
    {
        hql.append( ", " ).append( column );
    }
}
