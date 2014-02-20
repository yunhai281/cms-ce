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
        Assert.assertTrue( "range cannot be parsed!", "bytes=0-5".matches( HttpServletRangeUtil.PATTERN_RANGE ) );
        Assert.assertTrue( "range cannot be parsed!", "bytes=0-5,6-10,11-20".matches( HttpServletRangeUtil.PATTERN_RANGE ) );
        Assert.assertTrue( "range cannot be parsed!", "bytes=0-5, 6-10, 11-20".matches( HttpServletRangeUtil.PATTERN_RANGE ) );
        Assert.assertTrue( "range cannot be parsed!", "bytes=-500".matches( HttpServletRangeUtil.PATTERN_RANGE ) );
        Assert.assertTrue( "range cannot be parsed!", "bytes=-500".matches( HttpServletRangeUtil.PATTERN_RANGE ) );
        Assert.assertTrue( "range cannot be parsed!", "bytes=9500-".matches( HttpServletRangeUtil.PATTERN_RANGE ) );
        Assert.assertTrue( "range cannot be parsed!", "bytes=0-0,-1".matches( HttpServletRangeUtil.PATTERN_RANGE ) );
        Assert.assertTrue( "range cannot be parsed!", "bytes=500-600,601-999".matches( HttpServletRangeUtil.PATTERN_RANGE ) );
        Assert.assertTrue( "range cannot be parsed!", "bytes=42-1233/*".matches( HttpServletRangeUtil.PATTERN_RANGE ) );
    }
}
