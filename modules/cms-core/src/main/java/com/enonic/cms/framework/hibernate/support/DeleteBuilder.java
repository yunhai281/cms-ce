/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.framework.hibernate.support;

/**
 * Builds Select statements using HQL syntax.   This class can not be used for regular SQL.
 */
public class DeleteBuilder extends QueryBuilder
{

    public DeleteBuilder()
    {
        this.hql = new StringBuffer( "DELETE " );
    }

    public DeleteBuilder( int tabs )
    {
        this.hql = new StringBuffer( "DELETE " );
        this.tabs = tabs;
    }

    public DeleteBuilder( StringBuffer hql, int tabs )
    {
        this.hql = hql;
        this.tabs = tabs;
    }


}
