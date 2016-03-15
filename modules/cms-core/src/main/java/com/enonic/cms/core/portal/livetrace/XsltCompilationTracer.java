package com.enonic.cms.core.portal.livetrace;

public class XsltCompilationTracer
{

    public static XsltCompilationTrace startTracing( final LivePortalTraceService livePortalTraceService, final String template )
    {
        if ( !livePortalTraceService.tracingEnabled() )
        {
            return null;
        }

        return livePortalTraceService.startXsltCompilationTracing( template );
    }

    public static void startConcurrencyBlockTimer( XsltCompilationTrace trace )
    {
        if ( trace != null )
        {
            trace.startConcurrencyBlockTimer();
        }
    }

    public static void stopConcurrencyBlockTimer( XsltCompilationTrace trace )
    {
        if ( trace != null )
        {
            trace.stopConcurrencyBlockTimer();
        }
    }

    public static void setCached( final XsltCompilationTrace trace, final boolean cached )
    {
        if ( trace != null )
        {
            trace.setCached( cached );
        }
    }

    public static void stopTracing( final XsltCompilationTrace trace, final LivePortalTraceService livePortalTraceService )
    {
        if ( trace != null )
        {
            livePortalTraceService.stopTracing( trace );
        }
    }
}
