/*
 * Copyright 2000-2011 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.xslt.saxon;

import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;

import com.enonic.cms.core.xslt.XsltProcessor;
import com.enonic.cms.core.xslt.XsltProcessorErrors;
import com.enonic.cms.core.xslt.XsltProcessorException;
import com.enonic.cms.core.xslt.XsltProcessorManager;
import com.enonic.cms.core.xslt.XsltProcessorManagerAccessor;
import com.enonic.cms.core.xslt.XsltResource;
import com.enonic.cms.core.xslt.cache.TemplatesXsltCache;
import com.enonic.cms.core.xslt.functions.admin.AdminXsltFunctionLibrary;
import com.enonic.cms.core.xslt.functions.portal.PortalXsltFunctionLibrary;
import com.enonic.cms.core.xslt.lib.PortalFunctionsMediator;
import com.enonic.cms.core.xslt.localizer.LocalizerFactoryImpl;

/**
 * This class implements the standard xslt processor manager.
 */
@Component
public final class SaxonProcessorManager
    implements XsltProcessorManager
{
    private final Processor processor;

    private final Configuration configuration;

    private TemplatesXsltCache cache;

    public SaxonProcessorManager()
    {
        XsltProcessorManagerAccessor.setProcessorManager( this );

        this.configuration = new Configuration();
        this.configuration.setLineNumbering( true );
        this.configuration.setHostLanguage( Configuration.XSLT );
        this.configuration.setVersionWarning( false );
        this.configuration.setLocalizerFactory( new LocalizerFactoryImpl() );
        this.configuration.setCompileWithTracing( true );
        this.configuration.setValidationWarnings( true );

        this.processor = new Processor( this.configuration );
        new AdminXsltFunctionLibrary().registerAll( this.configuration );
    }

    public XsltProcessor createProcessor( final Source xsl, final URIResolver resolver )
        throws XsltProcessorException
    {
        return new XsltProcessorImpl( compileXslt( xsl, resolver ) );
    }

    public XsltProcessor createProcessor( final XsltResource xsl, final URIResolver resolver )
        throws XsltProcessorException
    {
        return createProcessor( xsl.getAsSource(), resolver );
    }

    @Override
    public XsltProcessor createCachedProcessor( final XsltResource xsl, final URIResolver resolver )
        throws XsltProcessorException
    {
        if ( this.cache == null )
        {
            return createProcessor( xsl, resolver );
        }

        XsltExecutable templates = this.cache.get( xsl );
        if ( templates == null )
        {
            templates = compileXslt( xsl.getAsSource(), resolver );
            this.cache.put( xsl, templates );
        }

        return new XsltProcessorImpl( templates );
    }

    private XsltExecutable compileXslt( final Source xsl, final URIResolver resolver )
        throws XsltProcessorException
    {
        final XsltProcessorErrors errors = new XsltProcessorErrors();

        try
        {
            final XsltCompiler compiler = this.processor.newXsltCompiler();
            compiler.setErrorListener( errors );
            compiler.setURIResolver( resolver );
            return compiler.compile( xsl );
        }
        catch ( final Exception e )
        {
            throw new XsltProcessorException( e, errors );
        }
    }

    @Autowired
    public void setTemplatesXsltCache( final TemplatesXsltCache cache )
    {
        this.cache = cache;
    }

    @Autowired
    public void setPortalFunctions( final PortalFunctionsMediator portalFunctions )
    {
        new PortalXsltFunctionLibrary( portalFunctions ).registerAll( this.configuration );
    }
}
