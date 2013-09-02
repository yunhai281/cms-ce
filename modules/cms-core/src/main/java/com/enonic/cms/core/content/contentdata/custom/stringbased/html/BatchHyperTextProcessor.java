/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.content.contentdata.custom.stringbased.html;

/**
 * runs chain of HyperTextProcessors
 */
public final class BatchHyperTextProcessor
    implements HyperTextProcessor
{
    private static final HyperTextProcessor[] processors = new HyperTextProcessor[]{new XMLDeclarationRemover(), new XHTMLValidator(), new XSSCleaner()};

    @Override
    public String prepare( final String name, final String value )
    {
        String processed = value;

        for ( final HyperTextProcessor processor : processors )
        {
            processed = processor.prepare( name, value );
        }

        return processed;
    }
}
