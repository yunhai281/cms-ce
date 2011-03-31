/*
 * Copyright 2000-2011 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.domain.log;


public class ContentLogEntrySpecification
    extends LogEntrySpecification
{

    private boolean allowDeletedContent = false;


    public void setAllowDeletedContent( boolean value )
    {
        this.allowDeletedContent = value;

    }

    public boolean isAllowDeletedContent()
    {
        return allowDeletedContent;
    }


}