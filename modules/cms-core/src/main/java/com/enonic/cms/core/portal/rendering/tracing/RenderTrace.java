/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.portal.rendering.tracing;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.google.common.base.Preconditions;

import com.enonic.cms.core.security.PortalSecurityHolder;
import com.enonic.cms.core.security.user.UserKey;
import com.enonic.cms.core.servlet.ServletRequestAccessor;
import com.enonic.cms.core.structure.portlet.PortletKey;

/**
 * This class manages the rendering trace.
 */
public final class RenderTrace
{
    /**
     * Context key.
     */
    private final static String CONTEXT_KEY = TraceContext.class.getName();

    /**
     * Return the current session.
     */
    private static HttpSession getSession()
    {
        return getCurrentRequest().getSession();
    }

    /**
     * Return the current trace context.
     */
    public static TraceContext getCurrentTraceContext()
    {
        return (TraceContext) getCurrentRequest().getAttribute( CONTEXT_KEY );
    }

    /**
     * Return the current request.
     */
    private static HttpServletRequest getCurrentRequest()
    {
        return ServletRequestAccessor.getRequest();
    }

    private static RenderTraceHistory getHistoryFromSession()
    {
        final UserKey userKey = PortalSecurityHolder.getLoggedInUser();
        final HttpSession session = getSession();

        final RenderTraceHistory history = RenderTraceHistory.getFromSession( session, userKey );
        Preconditions.checkState( history != null,
                                  "Could not find RenderTraceHistory for user [" + userKey + "] in session [" + session.getId() + "]" );
        return history;
    }

    private static RenderTraceHistory getOrCreateHistoryInSession()
    {
        final UserKey userKey = PortalSecurityHolder.getLoggedInUser();
        final HttpSession session = getSession();

        RenderTraceHistory history = RenderTraceHistory.getFromSession( session, userKey );
        if ( history != null )
        {
            return history;
        }

        history = new RenderTraceHistory();
        history.setInSession( session, userKey );
        return history;
    }

    public static void markRequestAsExecutedInDebugMode( final HttpServletRequest request )
    {
        if ( request != null )
        {
            request.setAttribute( "ICE", "ICE" );
        }
    }

    public static boolean isExecutingInDebugMode()
    {
        HttpServletRequest currentRequest = getCurrentRequest();
        return currentRequest != null && currentRequest.getAttribute( "ICE" ) != null;
    }

    /**
     * Start render trace.
     */
    public static void enter()
    {
        if ( !isExecutingInDebugMode() )
        {
            return;
        }

        RenderTraceInfo info = new RenderTraceInfo();

        final TraceContext context = new TraceContext( info );
        getCurrentRequest().setAttribute( CONTEXT_KEY, context );

        final RenderTraceHistory history = getOrCreateHistoryInSession();
        history.addFirst( info );

        info.enter();
    }

    /**
     * Stop render trace.
     */
    public synchronized static void exit()
    {
        if ( !isExecutingInDebugMode() )
        {
            return;
        }

        RenderTraceInfo renderTraceOnRequest = getCurrentRenderTraceInfo();
        getCurrentRequest().removeAttribute( CONTEXT_KEY );

        final RenderTraceHistory history = getOrCreateHistoryInSession();
        final RenderTraceInfo renderTraceInHistory = history.getRenderTraceInfo( renderTraceOnRequest.getKey() );

        if ( renderTraceInHistory != null )
        {
            renderTraceInHistory.exit();

            if ( renderTraceInHistory.getPageInfo() == null )
            {
                // Remove non page traces
                history.remove( renderTraceInHistory );
            }
        }

        history.ensureMaxSize();
    }

    /**
     * Enter page trace.
     */
    public static PageTraceInfo enterPage( int key )
    {
        TraceContext context = getCurrentTraceContext();
        if ( context != null )
        {
            PageTraceInfo info = new PageTraceInfo( key );
            context.setPageTraceInfo( info );
            info.enter();
            return info;
        }
        else
        {
            return null;
        }
    }

    /**
     * Exit page trace.
     */
    public static PageTraceInfo exitPage()
    {
        TraceContext context = getCurrentTraceContext();
        if ( context != null )
        {
            PageTraceInfo info = context.getPageTraceInfo();
            info.exit();
            return info;
        }
        else
        {
            return null;
        }
    }

    /**
     * Enter page object trace.
     */
    public static PagePortletTraceInfo enterPageObject( PortletKey key )
    {
        TraceContext context = getCurrentTraceContext();
        if ( context != null )
        {
            PagePortletTraceInfo info = new PagePortletTraceInfo( key );
            context.pushPageObjectTraceInfo( info );
            info.enter();
            return info;
        }
        else
        {
            return null;
        }
    }

    /**
     * Exit page object trace.
     */
    public static PagePortletTraceInfo exitPageObject()
    {
        TraceContext context = getCurrentTraceContext();
        if ( context != null )
        {
            PagePortletTraceInfo info = context.popPageObjectTraceInfo();
            info.exit();
            return info;
        }
        else
        {
            return null;
        }
    }

    /**
     * Enter function trace.
     */
    public static FunctionTraceInfo enterFunction( String name )
    {
        TraceContext context = getCurrentTraceContext();
        if ( context != null )
        {
            FunctionTraceInfo info = new FunctionTraceInfo( name );
            context.pushFunctionTraceInfo( info );
            info.enter();
            return info;
        }
        else
        {
            return null;
        }
    }

    /**
     * Exit function trace.
     */
    public static FunctionTraceInfo exitFunction()
    {
        TraceContext context = getCurrentTraceContext();
        if ( context != null )
        {
            FunctionTraceInfo info = context.popFunctionTraceInfo();
            info.exit();
            return info;
        }
        else
        {
            return null;
        }
    }

    /**
     * Return true if it is inside render trace.
     */
    public static boolean isTraceOn()
    {
        return getCurrentTraceContext() != null;
    }

    public static boolean isTraceOff()
    {
        return !isTraceOn();
    }

    /**
     * Return the current render trace info.
     */
    public static RenderTraceInfo getCurrentRenderTraceInfo()
    {
        TraceContext context = getCurrentTraceContext();
        return context != null ? context.getRenderTraceInfo() : null;
    }

    /**
     * Return the current render trace info.
     */
    public static PagePortletTraceInfo getCurrentPageObjectTraceInfo()
    {
        TraceContext context = getCurrentTraceContext();
        return context != null ? context.getCurrentPageObjectTraceInfo() : null;
    }

    /**
     * Return the current render trace info.
     */
    public static DataTraceInfo getCurrentDataTraceInfo()
    {
        TraceContext context = getCurrentTraceContext();
        return context != null ? context.getCurrentDataTraceInfo() : null;
    }

    /**
     * Return a render trace info by key.
     */
    public synchronized static RenderTraceInfo getRenderTraceInfo( String key )
    {
        return getHistoryFromSession().getRenderTraceInfo( key );
    }
}
