package com.enonic.cms.core.xslt;

import org.junit.Test;

import static org.junit.Assert.*;

public class XsltResourceHelperTest
{

    @Test
    public void testAbsolutePathWithSpace()
    {
        String href = "/modules/library stk/system copy.xsl";
        String base = "dummy:/%2Fmodules%2Ftheme+sample+site%2Fpage.xsl";
        String expected = "/modules/library stk/system copy.xsl";
        String result = XsltResourceHelper.resolveRelativePath( href, base );
        assertEquals( expected, result );

    }

    @Test
    public void testCreateUriWithPlus()
    {
        String url = "/modules/TINE+GAARDER/main page.xsl";
        String expected = "dummy:/%2Fmodules%2FTINE%2BGAARDER%2Fmain+page.xsl";
        String result = XsltResourceHelper.createUri( url );
        assertEquals( expected, result );
    }

    @Test
    public void testResolveRelativePathWithSpace()
    {
        String href = "system copy.xsl";
        String base = "dummy:/%2Fmodules%2Ftheme+sample+site%2Fpage.xsl";
        String expected = "/modules/theme sample site/system copy.xsl";
        String result = XsltResourceHelper.resolveRelativePath( href, base );
        assertEquals( expected, result );
    }

    @Test
    public void testResolveRelativePathWithPlus()
    {
        String href = "main+page.xsl";
        String base = "dummy:/%2Fmodules%2Ftheme-sample-site%2Fpage.xsl";
        String expected = "/modules/theme-sample-site/main+page.xsl";
        String result = XsltResourceHelper.resolveRelativePath( href, base );
        assertEquals( expected, result );
    }
}
