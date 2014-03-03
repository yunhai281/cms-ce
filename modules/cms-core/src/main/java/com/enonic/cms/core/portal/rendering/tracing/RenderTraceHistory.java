/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.portal.rendering.tracing;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.LinkedList;

import javax.servlet.http.HttpSession;

import com.google.common.collect.Lists;

import com.enonic.cms.core.security.user.UserKey;

/**
 * This class is used as a serializable render trace history. It does not actually serialize the history, but
 * allows the history to be saved in session without errors.
 */
public final class RenderTraceHistory
    implements Externalizable
{
    /**
     * History size.
     */
    public final static int MAX_SIZE = 20;

    private final static String HISTORY_PREFIX = "HISTORY_";

    private transient LinkedList<RenderTraceInfo> history;

    public RenderTraceHistory()
    {
        this.history = Lists.newLinkedList();
    }

    public void setInSession( final HttpSession session, final UserKey userKey )
    {
        final String key = HISTORY_PREFIX + userKey;
        session.setAttribute( key, this );
    }

    @Override
    public void writeExternal( final ObjectOutput out )
        throws IOException
    {
        // Do nothing
    }

    @Override
    public void readExternal( final ObjectInput in )
        throws IOException, ClassNotFoundException
    {
        this.history = Lists.newLinkedList();
    }

    public LinkedList<RenderTraceInfo> getHistory()
    {
        return history;
    }

    public synchronized RenderTraceInfo getRenderTraceInfo( String key )
    {
        for ( RenderTraceInfo renderTraceInfo : this.history )
        {
            if ( renderTraceInfo.getKey().equals( key ) )
            {
                return renderTraceInfo;
            }
        }
        return null;
    }

    public synchronized void addFirst( final RenderTraceInfo renderTraceInfo )
    {
        this.history.addFirst( renderTraceInfo );
    }

    public synchronized void remove( final RenderTraceInfo renderTraceInfo )
    {
        this.history.remove( renderTraceInfo );
    }

    public synchronized void ensureMaxSize()
    {
        while ( history.size() > MAX_SIZE )
        {
            history.removeLast();
        }
    }

    public static RenderTraceHistory getFromSession( final HttpSession session, final UserKey userKey )
    {
        final String key = HISTORY_PREFIX + userKey;
        final Object value = session.getAttribute( key );

        if ( value instanceof RenderTraceHistory )
        {
            return (RenderTraceHistory) value;
        }
        else
        {
            return null;
        }
    }

    public Object getSize()
    {
        return history.size();
    }
}
