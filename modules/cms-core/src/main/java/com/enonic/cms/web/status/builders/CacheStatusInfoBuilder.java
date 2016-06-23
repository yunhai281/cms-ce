package com.enonic.cms.web.status.builders;

import org.codehaus.jackson.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.enonic.cms.framework.cache.CacheFacade;
import com.enonic.cms.framework.cache.CacheManager;

import com.enonic.cms.web.status.StatusInfoBuilder;

@Component
public class CacheStatusInfoBuilder
    extends StatusInfoBuilder
{

    @Autowired
    private CacheManager cacheManager;

    public CacheStatusInfoBuilder()
    {
        super( "cache" );
    }

    @Override
    protected void build( final ObjectNode json )
    {
        final Iterable<CacheFacade> caches = this.cacheManager.getAll();

        for ( final CacheFacade cache : caches )
        {
            createCacheEntryJson( json, cache );
        }
    }

    private void createCacheEntryJson( final ObjectNode json, final CacheFacade cache )
    {
        final ObjectNode cacheEntry = json.putObject( cache.getName() );
        cacheEntry.put( "capacity", cache.getMemoryCapacity() );
        cacheEntry.put( "capacityUsage", cache.getMemoryCapacityUsage() );
        cacheEntry.put( "elements", cache.getCount() );
        cacheEntry.put( "hits", cache.getHitCount() );
        cacheEntry.put( "misses", cache.getMissCount() );
        cacheEntry.put( "clears", cache.getRemoveAllCount() );
        cacheEntry.put( "effectiveness", cache.getEffectiveness() );

    }
}
