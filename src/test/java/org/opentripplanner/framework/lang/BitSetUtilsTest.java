package org.opentripplanner.framework.lang;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class BitSetUtilsTest {

  @Test
  public void testBitSetUtils() {
    byte bflags = 0;
    short sflags = 0;
    int iflags = 0;
    long lflags = 0L;

    for (int i = 0; i < 8; i++) {
      assertFalse(BitSetUtils.get(bflags, i));
      bflags = BitSetUtils.set(bflags, i, true);
      assertTrue(BitSetUtils.get(bflags, i));
      bflags = BitSetUtils.set(bflags, i, false);
      assertFalse(BitSetUtils.get(bflags, i));
    }
    for (int i = 0; i < 16; i++) {
      assertFalse(BitSetUtils.get(sflags, i));
      sflags = BitSetUtils.set(sflags, i, true);
      assertTrue(BitSetUtils.get(sflags, i));
      sflags = BitSetUtils.set(sflags, i, false);
      assertFalse(BitSetUtils.get(sflags, i));
    }
    for (int i = 0; i < 32; i++) {
      assertFalse(BitSetUtils.get(iflags, i));
      iflags = BitSetUtils.set(iflags, i, true);
      assertTrue(BitSetUtils.get(iflags, i));
      iflags = BitSetUtils.set(iflags, i, false);
      assertFalse(BitSetUtils.get(iflags, i));
    }
    for (int i = 0; i < 64; i++) {
      assertFalse(BitSetUtils.get(lflags, i));
      lflags = BitSetUtils.set(lflags, i, true);
      assertTrue(BitSetUtils.get(lflags, i));
      lflags = BitSetUtils.set(lflags, i, false);
      assertFalse(BitSetUtils.get(lflags, i));
    }
  }
}
