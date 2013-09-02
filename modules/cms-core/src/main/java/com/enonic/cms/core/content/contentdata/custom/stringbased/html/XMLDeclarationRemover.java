/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.content.contentdata.custom.stringbased.html;

import org.apache.commons.lang.StringUtils;

/**
 * removes &lt;?xml ... ?&gt; from beginning of XML file
 */
final class XMLDeclarationRemover
    implements HyperTextProcessor
{
    @Override
    public String prepare( final String name, final String value )
    {
        if ( StringUtils.isEmpty( value ) )
        {
            return value;
        }

        final String start = "<?xml";
        final String end = "?>";
        if ( !value.startsWith( start ) )
        {
            return value;
        }
        final int endPos = value.indexOf( end );
        if ( endPos < 0 )
        {
            return value;
        }
        return value.substring( endPos + end.length() ).trim();
    }
}
