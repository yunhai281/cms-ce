/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.content.contentdata.custom.stringbased.html;

import java.io.InputStream;

import org.owasp.validator.html.AntiSamy;
import org.owasp.validator.html.CleanResults;
import org.owasp.validator.html.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * removes &lt;script&gt; and other xss tags.
 * <p/>
 * uses AntiSamy for parsing.
 */
final class XSSCleaner
    implements HyperTextProcessor
{
    private static final Logger LOG = LoggerFactory.getLogger( XSSCleaner.class );

    private static final String DEFAULT_CONFIG_FILE = "/antisamy-tinymce-1.4.4.xml";

    private final AntiSamy antiSamy;

    public XSSCleaner()
    {
        antiSamy = new AntiSamy( createPolicy() );
    }

    private Policy createPolicy()
    {
        try
        {
            final InputStream configFileStream = getClass().getResourceAsStream( DEFAULT_CONFIG_FILE );

            return Policy.getInstance( configFileStream );
        }
        catch ( Exception e )
        {
            LOG.error( "cannot load antisamy config file.", e );
            return null;
        }

    }

    @Override
    public String prepare( final String name, final String value )
    {
        try
        {
            final CleanResults cleanResults = antiSamy.scan( value );
            return cleanResults.getCleanHTML().trim();
        }
        catch ( Exception e )
        {
            LOG.error( "cannot process HTML data", e );
            return value;
        }
    }
}
