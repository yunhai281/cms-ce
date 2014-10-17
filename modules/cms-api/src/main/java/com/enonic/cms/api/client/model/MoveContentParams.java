package com.enonic.cms.api.client.model;

public class MoveContentParams
    extends AbstractParams
{
    private static final long serialVersionUID = -343625349023838018L;

    /**
     * The key of the content to move.
     */
    public Integer contentKey;

    /**
     * The key of the category that the content should be moved to.
     */
    public Integer categoryKey;
}
