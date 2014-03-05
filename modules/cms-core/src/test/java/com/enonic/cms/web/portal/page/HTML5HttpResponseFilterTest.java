/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.web.portal.page;

import org.junit.Assert;
import org.junit.Test;


public class HTML5HttpResponseFilterTest
{
    @Test
    public void test_fixer_process()
        throws Exception
    {
        final String html = " <!DOCTYPE html \r\n SYSTEM  \r \"about:legacy-compat\">test";

        Assert.assertEquals( "<!DOCTYPE html>test", new HTML5HttpResponseFilter( "html5fixer" ).filterResponse( null, html, "text/html" ) );
    }

    @Test
    public void test_fixer_skip()
        throws Exception
    {
        final String html = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">test";

        Assert.assertSame( html, new HTML5HttpResponseFilter( "html5fixer" ).filterResponse( null, html, "text/html" ) );

    }

    @Test
    public void test_forcer_process()
        throws Exception
    {
        final String html = "         \r \n  \r\n \n\r     <!DOCTYPE html \r\n SYSTEM  \r \"about:legacy-compat\">test";

        Assert.assertEquals( "<!DOCTYPE html>test",
                             new HTML5HttpResponseFilter( "html5forcer" ).filterResponse( null, html, "text/html" ) );
    }

    @Test
    public void test_forcer_process_too()
        throws Exception
    {
        final String html =

            "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">test";
        Assert.assertEquals( "<!DOCTYPE html>test",
                             new HTML5HttpResponseFilter( "html5forcer" ).filterResponse( null, html, "text/html" ) );

    }

    @Test
    public void test_forcer_skip_frameset()
        throws Exception
    {
        final String pattern =
            "<!DOCTYPE html\n  PUBLIC \"-//W3C//DTD HTML 4.01 Frameset//EN\" \"http://www.w3.org/TR/html4/frameset.dtd\">test";

        Assert.assertSame( pattern, new HTML5HttpResponseFilter( "html5fixer" ).filterResponse( null, pattern, "text/html" ) );
    }

}
