package org.opentripplanner.common;


import org.junit.Assert;
import org.junit.Test;

import static org.opentripplanner.common.LoggingUtil.fileSizeToString;

public class LoggingUtilTest {

    @Test
    public void testFileSizeToString() {
        Assert.assertEquals("1 byte", fileSizeToString(1));
        Assert.assertEquals("12 bytes", fileSizeToString(12));
        Assert.assertEquals("123 bytes", fileSizeToString(123));
        Assert.assertEquals("1 kb", fileSizeToString(1234));
        Assert.assertEquals("12 kb", fileSizeToString(12345));
        Assert.assertEquals("123 kb", fileSizeToString(123456));
        Assert.assertEquals("1.2 MB", normalize(fileSizeToString(1234567)));
        Assert.assertEquals("12.3 MB", normalize(fileSizeToString(12345678)));
        // Round up
        Assert.assertEquals("123.5 MB", normalize(fileSizeToString(123456789)));
        Assert.assertEquals("1.2 GB", normalize(fileSizeToString(1234567890)));
        Assert.assertEquals("12.3 GB", normalize(fileSizeToString(12345678901L)));
    }

    private static String normalize(String number) {
        return number.replace(',', '.');
    }
}
