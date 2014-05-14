package com.enonic.cms.web.main;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.enonic.cms.web.error.ErrorDetails;
import com.enonic.cms.web.error.ErrorPageRenderer;

@Controller
public final class ErrorController
{
    @Autowired
    protected ErrorPageRenderer errorPageRenderer;

    @RequestMapping(value = "/error", method = {RequestMethod.GET,RequestMethod.HEAD}, produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView error( final HttpServletRequest req, final HttpServletResponse res )
        throws IOException
    {
        this.errorPageRenderer.render( res, createDetails( req ) );
        return null;
    }

    private int getStatusCode( final HttpServletRequest req )
    {
        final Object statusCode = req.getAttribute( "javax.servlet.error.status_code" );
        if ( statusCode instanceof Integer )
        {
            return (Integer) statusCode;
        }

        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }

    private Throwable getCauseException( final HttpServletRequest req, final int statusCode )
    {
        final Object cause = req.getAttribute( "javax.servlet.error.exception" );
        if ( cause instanceof Throwable )
        {
            return (Throwable) cause;
        }

        return new Exception( getStandardMessage( statusCode ) );
    }

    private String getStandardMessage( final int statusCode )
    {
        if ( statusCode == HttpServletResponse.SC_NOT_FOUND )
        {
            return "Resource not found";
        }

        return "An Error Occured (" + statusCode + ")";
    }

    private ErrorDetails createDetails( final HttpServletRequest req )
    {
        final int statusCode = getStatusCode( req );
        final Throwable cause = getCauseException( req, statusCode );
        return new ErrorDetails( req, cause, statusCode );
    }
}
