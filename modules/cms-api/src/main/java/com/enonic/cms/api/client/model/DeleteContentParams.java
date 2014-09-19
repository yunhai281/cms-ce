/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.api.client.model;

import java.io.Serializable;


public class DeleteContentParams
    extends AbstractParams
    implements Serializable
{

    private static final long serialVersionUID = 3249551916910973316L;

    /**
     * The key of the content to delete.
     */
    public Integer contentKey;

    /**
     * The siteKey is used to report the context of the event to the event log. If ignored or set to null, the event log will report the
     * event on the admin console.
     */
    public Integer siteKey;


}