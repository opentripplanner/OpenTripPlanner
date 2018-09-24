package org.opentripplanner.util;

import junit.framework.TestCase;

public class BitSetUtilsTest extends TestCase {

    public void testBitSetUtils() {

        byte bflags = 0;
        short sflags = 0;
        int iflags = 0;
        long lflags = 0L;

        for (int i = 0; i < 8; i++) {
            assertEquals(false, BitSetUtils.get(bflags, i));
            bflags = BitSetUtils.set(bflags, i, true);
            assertEquals(true, BitSetUtils.get(bflags, i));
            bflags = BitSetUtils.set(bflags, i, false);
            assertEquals(false, BitSetUtils.get(bflags, i));
        }
        for (int i = 0; i < 16; i++) {
            assertEquals(false, BitSetUtils.get(sflags, i));
            sflags = BitSetUtils.set(sflags, i, true);
            assertEquals(true, BitSetUtils.get(sflags, i));
            sflags = BitSetUtils.set(sflags, i, false);
            assertEquals(false, BitSetUtils.get(sflags, i));
        }
        for (int i = 0; i < 32; i++) {
            assertEquals(false, BitSetUtils.get(iflags, i));
            iflags = BitSetUtils.set(iflags, i, true);
            assertEquals(true, BitSetUtils.get(iflags, i));
            iflags = BitSetUtils.set(iflags, i, false);
            assertEquals(false, BitSetUtils.get(iflags, i));
        }
        for (int i = 0; i < 64; i++) {
            assertEquals(false, BitSetUtils.get(lflags, i));
            lflags = BitSetUtils.set(lflags, i, true);
            assertEquals(true, BitSetUtils.get(lflags, i));
            lflags = BitSetUtils.set(lflags, i, false);
            assertEquals(false, BitSetUtils.get(lflags, i));
        }
    }
}
