package com.enonic.cms.web.error;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

public interface ErrorPageRenderer
{
    public void render( HttpServletResponse res, ErrorDetails details )
        throws IOException;
}
