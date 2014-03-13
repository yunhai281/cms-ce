package com.enonic.cms.web.error;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import com.enonic.cms.core.portal.rendering.tracing.RenderTrace;
import com.enonic.cms.web.portal.template.TemplateProcessor;

@Component
public final class ErrorPageRendererImpl
    implements ErrorPageRenderer
{
    @Autowired
    protected TemplateProcessor templateProcessor;

    @Value("${cms.error.page.detailInformation}")
    protected boolean detailInformation;

    @Override
    public void render( final HttpServletResponse res, final ErrorDetails details )
        throws IOException
    {
        final Map<String, Object> model = Maps.newHashMap();
        model.put( "details", details );

        final String result = this.templateProcessor.process( getTemplateName(), model );

        res.setStatus( details.getStatusCode() );
        res.setContentType( "text/html" );
        res.setCharacterEncoding( "UTF-8" );
        res.getWriter().println( result );
    }

    private String getTemplateName()
    {
        final boolean details = this.detailInformation || RenderTrace.isExecutingInDebugMode();
        return details ? "errorPage.ftl" : "errorPageMinimal.ftl";
    }
}
