package com.enonic.cms.core.portal.livetrace;

public class XsltCompilationTrace
    extends BaseTrace
    implements Trace
{
    static final int CONCURRENCY_BLOCK_THRESHOLD = 5;

    private long concurrencyBlockStartTime = 0;

    private long concurrencyBlockingTime = 0;

    private boolean cached = false;

    private final String template;

    public XsltCompilationTrace( final String template )
    {
        this.template = template;
    }

    public boolean isConcurrencyBlocked()
    {
        return concurrencyBlockingTime > CONCURRENCY_BLOCK_THRESHOLD;
    }

    public long getConcurrencyBlockingTime()
    {
        return concurrencyBlockingTime;
    }

    public boolean isCached()
    {
        return cached;
    }

    public void setCached( final boolean cached )
    {
        this.cached = cached;
    }

    void startConcurrencyBlockTimer()
    {
        concurrencyBlockStartTime = System.currentTimeMillis();
    }

    void stopConcurrencyBlockTimer()
    {
        this.concurrencyBlockingTime = System.currentTimeMillis() - concurrencyBlockStartTime;
    }

    public String getTemplate()
    {
        return template;
    }
}
