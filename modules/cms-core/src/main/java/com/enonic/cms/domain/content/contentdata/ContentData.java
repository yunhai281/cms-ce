/*
 * Copyright 2000-2011 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.domain.content.contentdata;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.enonic.cms.domain.content.ContentKey;
import com.enonic.cms.domain.content.binary.BinaryDataKey;


public interface ContentData
{
    String getTitle();

    void replaceBinaryKeyPlaceholders( List<BinaryDataKey> binaryDatas );

    Set<ContentKey> resolveRelatedContentKeys();

    boolean hasRelatedChild( ContentKey contentKey );

    void turnBinaryKeysIntoPlaceHolders( Map<BinaryDataKey, Integer> indexByBinaryDataKey );
}
