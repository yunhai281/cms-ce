/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.content.contentdata.custom.stringbased.html;


/**
 * does changes on html before saving to database
 *
 * @see BatchHyperTextProcessor
 */
public interface HyperTextProcessor
{
    String prepare( final String name, final String html );
}



