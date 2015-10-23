/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.store.dao;

import java.util.List;

import com.enonic.cms.core.content.contenttype.ContentHandlerKey;
import com.enonic.cms.core.content.contenttype.ContentTypeEntity;
import com.enonic.cms.core.content.contenttype.ContentTypeKey;
import com.enonic.cms.core.resource.ResourceKey;
import com.enonic.cms.store.support.EntityPageList;

public interface ContentTypeDao
    extends EntityDao<ContentTypeEntity>
{
    @Deprecated
    ContentTypeEntity findByKey( int key );

    ContentTypeEntity findByKey( ContentTypeKey key );

    ContentTypeEntity findByName( String name );

    List<ContentTypeEntity> findByContentHandler( ContentHandlerKey contentHandlerKey );

    List<ContentTypeEntity> getAll();

    List getResourceUsageCountCSS();

    List<ContentTypeEntity> findByCSS( ResourceKey resourceKey );

    void updateResourceCSSReference( ResourceKey oldResourceKey, ResourceKey newResourceKey );

    void updateResourceCSSReferencePrefix( String oldPrefix, String newPrefix );

    EntityPageList<ContentTypeEntity> findAll( int index, int count );
}
