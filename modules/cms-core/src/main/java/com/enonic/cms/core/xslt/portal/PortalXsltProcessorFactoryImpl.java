/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.xslt.portal;

import java.util.concurrent.locks.Lock;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.enonic.cms.framework.cache.CacheFacade;
import com.enonic.cms.framework.cache.CacheManager;
import com.enonic.cms.framework.util.GenericConcurrencyLock;

import com.enonic.cms.core.portal.livetrace.LivePortalTraceService;
import com.enonic.cms.core.portal.livetrace.XsltCompilationTrace;
import com.enonic.cms.core.portal.livetrace.XsltCompilationTracer;
import com.enonic.cms.core.resource.FileResourceName;
import com.enonic.cms.core.resource.FileResourceService;
import com.enonic.cms.core.xslt.XsltProcessorException;
import com.enonic.cms.core.xslt.base.SaxonXsltProcessorFactory;
import com.enonic.cms.core.xslt.functions.portal.PortalXsltFunctionLibrary;
import com.enonic.cms.core.xslt.lib.PortalFunctionsMediator;

@Component
public final class PortalXsltProcessorFactoryImpl
    extends SaxonXsltProcessorFactory
    implements PortalXsltProcessorFactory, InitializingBean
{
    private XsltResourceLoader resourceLoader;

    private XsltTemplatesCache templatesCache;

    private FileResourceService resourceService;

    private CacheFacade cacheFacade;

    private long checkInterval = 5000;

    private LivePortalTraceService livePortalTraceService;

    private static final Logger LOG = LoggerFactory.getLogger( PortalXsltProcessorFactory.class );

    private static GenericConcurrencyLock<String> concurrencyLock = GenericConcurrencyLock.create();

    @Autowired
    public void setPortalFunctions( final PortalFunctionsMediator portalFunctions )
    {
        addFunctionLibrary( new PortalXsltFunctionLibrary( portalFunctions ) );
    }

    @Override
    public PortalXsltProcessor createProcessor( final FileResourceName name )
        throws XsltProcessorException
    {
        final XsltCompilationTrace trace = XsltCompilationTracer.startTracing( livePortalTraceService, name.toString() );

        final XsltTrackingUriResolver uriResolver = new XsltTrackingUriResolver( this.resourceLoader );
        final XsltTemplatesCacheEntry templates = compileTemplates( name, uriResolver, trace );
        final Transformer transformer = createTransformer( templates, uriResolver );

        XsltCompilationTracer.stopTracing( trace, livePortalTraceService );

        return new PortalXsltProcessorImpl( transformer );
    }

    private XsltTemplatesCacheEntry compileTemplates( final FileResourceName name, final XsltTrackingUriResolver resolver,
                                                      final XsltCompilationTrace trace )
        throws XsltProcessorException
    {
        XsltTemplatesCacheEntry entry = this.templatesCache.get( name );
        if ( entry != null )
        {
            XsltCompilationTracer.setCached( trace, true );
            return entry;
        }

        final Lock locker = concurrencyLock.getLock( name.toString() );

        try
        {
            XsltCompilationTracer.startConcurrencyBlockTimer( trace );
            locker.lock();
            XsltCompilationTracer.stopConcurrencyBlockTimer( trace );

            entry = this.templatesCache.get( name );
            if ( entry != null )
            {
                XsltCompilationTracer.setCached( trace, true );
                return entry;
            }

            final Source xsl = loadResource( name );
            final Templates templates = compileTemplate( xsl, resolver );

            entry = new XsltTemplatesCacheEntry( name, templates );
            entry.addIncludes( resolver.getIncludes() );
            this.templatesCache.put( entry );

            XsltCompilationTracer.setCached( trace, false );
            return entry;
        }
        finally
        {
            locker.unlock();
        }

    }

    private Source loadResource( final FileResourceName name )
        throws XsltProcessorException
    {
        try
        {
            return this.resourceLoader.load( name );
        }
        catch ( final TransformerException e )
        {
            throw new XsltProcessorException( e );
        }
    }

    @Autowired
    public void setResourceService( final FileResourceService resourceService )
    {
        this.resourceService = resourceService;
    }

    @Autowired
    public void setCacheManager( final CacheManager cacheManager )
    {
        this.cacheFacade = cacheManager.getXsltCache();
    }

    @Value("${cms.cache.xslt.checkInterval}")
    public void setCheckInterval( final long checkInterval )
    {
        this.checkInterval = checkInterval;
    }

    @Autowired
    public void setLivePortalTraceService( final LivePortalTraceService livePortalTraceService )
    {
        this.livePortalTraceService = livePortalTraceService;
    }

    @Override
    public void afterPropertiesSet()
    {
        this.templatesCache = new XsltTemplatesCache( this.cacheFacade, this.resourceService, this.checkInterval );
        this.resourceLoader = new XsltResourceLoader( this.resourceService );
    }
}
