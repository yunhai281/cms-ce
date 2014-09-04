/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.api.client.model;

import java.io.Serializable;
import java.util.Date;

import com.enonic.cms.api.client.model.content.ContentDataInput;
import com.enonic.cms.api.client.model.content.ContentStatus;


public class CreateContentParams
    extends AbstractParams
    implements Serializable
{
    private static final long serialVersionUID = 9129880828683283644L;

    public Integer categoryKey;

    public Date publishFrom;

    public Date publishTo;

    /**
     * Default is DRAFT.
     *
     * @see com.enonic.cms.api.client.model.content.ContentStatus
     */
    public Integer status = ContentStatus.STATUS_DRAFT;

    public ContentDataInput contentData;

    public String changeComment;

    /**
     * The siteKey is used to report the context of the event to the event log.  If ignored or set to null, the eventlog will report the
     * event on the admin console.
     */
    public Integer siteKey;
}