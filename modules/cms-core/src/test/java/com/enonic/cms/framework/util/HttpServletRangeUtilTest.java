/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.framework.util;

import org.junit.Assert;
import org.junit.Test;

public class HttpServletRangeUtilTest
{
    @Test
    public void testRange()
        throws Exception
    {
        Assert.assertTrue("", "bytes=0-5".matches( HttpServletRangeUtil.PATTERN_RANGE ));
        Assert.assertTrue("", "bytes=0-5,6-10,11-20".matches( HttpServletRangeUtil.PATTERN_RANGE ));
        Assert.assertTrue("", "bytes=0-5, 6-10, 11-20".matches( HttpServletRangeUtil.PATTERN_RANGE ));
    }
}
