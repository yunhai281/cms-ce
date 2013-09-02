/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.content.contentdata.custom.stringbased.html;

import org.apache.commons.lang.StringUtils;
import org.jdom.Document;

import com.enonic.cms.framework.util.JDOMUtil;

import com.enonic.cms.core.content.contentdata.InvalidContentDataException;

/**
 *  validates if text is valid XHTML document
 *
 *  notes: will throw exception if html is not valid xthml document
 */
final public class XHTMLValidator
    implements HyperTextProcessor
{
    private static final String wrapElementName = "temp-wrapped-element-if-you-can-see-me-something-went-wrong";

    private static final String startWrap = "<" + wrapElementName + ">";

    private static final String endWrap = "</" + wrapElementName + ">";

    @Override
    public String prepare( final String name, final String value )
    {
        if ( StringUtils.isEmpty( value ) )
        {
            return value;
        }

        parseDocument( name, value );

        return value;
    }

    public static Document parseDocument( final String name, final String value )
    {
        try
        {
            return JDOMUtil.parseDocument( startWrap + value + endWrap );
        }
        catch ( final Exception e )
        {
            throw new InvalidContentDataException( "Input " + name + " has no valid xhtml value", e );
        }
    }

}
