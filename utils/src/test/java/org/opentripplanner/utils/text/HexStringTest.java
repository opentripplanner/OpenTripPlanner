package org.opentripplanner.utils.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HexStringTest {

  @Test
  void testEmptyString() {
    assertEquals("", HexString.of(new byte[] {}));
  }

  @Test
  void testZero() {
    assertEquals("00", HexString.of(new byte[] { (byte) 0x00 }));
  }

  @Test
  void testSingleElement() {
    assertEquals("ff", HexString.of(new byte[] { (byte) 0xff }));
  }

  @Test
  void testLongString() {
    assertEquals(
      "0123456789abcdef",
      HexString.of(
        new byte[] {
          (byte) 0x01,
          (byte) 0x23,
          (byte) 0x45,
          (byte) 0x67,
          (byte) 0x89,
          (byte) 0xab,
          (byte) 0xcd,
          (byte) 0xef,
        }
      )
    );
  }
}
