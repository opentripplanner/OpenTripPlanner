package org.opentripplanner.street.geometry;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class DlugoszVarLenIntPackerTest {

  /**
   * Pack/unpack round-trip across the boundary values of every varint width. Each width (7, 14,
   * 21, 27, 35 bits) is exercised at its upper limit and at the next value that forces a promotion
   * to the wider encoding, in both signs. The expected packed length asserts that the promotion
   * happens at the documented boundary.
   */
  @Test
  void packRoundTripCoversAllVarintWidths() {
    packTest(new int[] {}, 0);
    packTest(new int[] { 0 }, 1);
    packTest(new int[] { 63 }, 1);
    packTest(new int[] { -64 }, 1);
    packTest(new int[] { 64 }, 2);
    packTest(new int[] { -65 }, 2);
    packTest(new int[] { -8192 }, 2);
    packTest(new int[] { -8193 }, 3);
    packTest(new int[] { 8191 }, 2);
    packTest(new int[] { 8192 }, 3);
    packTest(new int[] { -1048576 }, 3);
    packTest(new int[] { -1048577 }, 4);
    packTest(new int[] { 1048575 }, 3);
    packTest(new int[] { 1048576 }, 4);
    packTest(new int[] { -67108864 }, 4);
    packTest(new int[] { -67108865 }, 5);
    packTest(new int[] { 67108863 }, 4);
    packTest(new int[] { 67108864 }, 5);
    packTest(new int[] { Integer.MAX_VALUE }, 5);
    packTest(new int[] { Integer.MIN_VALUE }, 5);

    packTest(new int[] { 0, 0 }, 2);
    packTest(new int[] { 0, 0, 0 }, 3);

    packTest(new int[] { 8100, 8200, 8300 }, 8);
  }

  @Test
  void unpackNullReturnsNull() {
    assertNull(DlugoszVarLenIntPacker.unpack(null));
  }

  @Test
  void packNullReturnsNull() {
    assertNull(DlugoszVarLenIntPacker.pack(null));
  }

  @Test
  void countValuesNullReturnsZero() {
    assertEquals(0, DlugoszVarLenIntPacker.countValues(null));
  }

  @Test
  void countValuesEmptyArrayReturnsZero() {
    assertEquals(0, DlugoszVarLenIntPacker.countValues(new byte[0]));
  }

  /**
   * {@link DlugoszVarLenIntPacker#countValues(byte[])} must yield the same count that
   * {@link DlugoszVarLenIntPacker#unpack(byte[])} would have produced, without decoding any
   * values. Allocation-free streaming decoders rely on this contract to pre-size their output
   * buffer.
   */
  @Test
  void countValuesMatchesUnpackLengthAcrossWidths() {
    int[][] cases = {
      new int[] {},
      new int[] { 0 },
      new int[] { 0, 100, -100 },
      // All five widths plus the int extremes
      new int[] {
        -64,
        64,
        -8192,
        8192,
        -1048576,
        1048576,
        -67108864,
        67108864,
        Integer.MAX_VALUE,
        Integer.MIN_VALUE,
      },
    };
    for (int[] values : cases) {
      byte[] packed = DlugoszVarLenIntPacker.pack(values);
      assertEquals(
        values.length,
        DlugoszVarLenIntPacker.countValues(packed),
        "countValues mismatch for " + Arrays.toString(values)
      );
      assertEquals(
        values.length,
        DlugoszVarLenIntPacker.unpack(packed).length,
        "unpack length mismatch for " + Arrays.toString(values)
      );
    }
  }

  /**
   * A {@link DlugoszVarLenIntPacker.Decoder} must produce the encoded value and consume exactly the
   * bytes of the varint, for every encoding width. Verified at the boundary values that uniquely
   * identify each width: after a single {@link DlugoszVarLenIntPacker.Decoder#next()} the cursor
   * must be exhausted for a one-element packing.
   */
  @Test
  void decoderReturnsValueAndAdvancesPositionForEveryWidth() {
    int[][] cases = {
      // { value, expected width in bytes }
      { 0, 1 },
      { 63, 1 },
      { -64, 1 },
      { 64, 2 },
      { -65, 2 },
      { 8191, 2 },
      { -8192, 2 },
      { 8192, 3 },
      { -8193, 3 },
      { 1048575, 3 },
      { -1048576, 3 },
      { 1048576, 4 },
      { -1048577, 4 },
      { 67108863, 4 },
      { -67108864, 4 },
      { 67108864, 5 },
      { -67108865, 5 },
      { Integer.MAX_VALUE, 5 },
      { Integer.MIN_VALUE, 5 },
    };
    for (int[] c : cases) {
      int value = c[0];
      int expectedWidth = c[1];
      byte[] packed = DlugoszVarLenIntPacker.pack(new int[] { value });
      assertEquals(expectedWidth, packed.length, "packed width for value " + value);
      var decoder = new DlugoszVarLenIntPacker.Decoder(packed);
      assertTrue(decoder.hasNext(), "decoder should have a value for input " + value);
      assertEquals(value, decoder.next(), "decoded value for input " + value);
      assertFalse(decoder.hasNext(), "decoder should be exhausted after one value for " + value);
    }
  }

  /**
   * Decoding a multi-element array that mixes every varint width exercises sequential decode
   * through all five branches and verifies the position threads correctly between them. A short
   * 7-bit value at the end confirms the decoder did not silently overshoot the previous 35-bit
   * varint.
   */
  @Test
  void unpackMixedWidthsRoundTrip() {
    int[] mixed = { 7, 8000, 1000000, 67000000, Integer.MAX_VALUE, -3 };
    byte[] packed = DlugoszVarLenIntPacker.pack(mixed);
    assertEquals(1 + 2 + 3 + 4 + 5 + 1, packed.length);
    assertArrayEquals(mixed, DlugoszVarLenIntPacker.unpack(packed));
    assertEquals(mixed.length, DlugoszVarLenIntPacker.countValues(packed));
  }

  /**
   * Walking a {@link DlugoszVarLenIntPacker.Decoder} with {@code hasNext()}/{@code next()} must
   * visit the entire array and produce exactly the values fed to {@code pack}, regardless of width
   * mix.
   */
  @Test
  void decoderCanWalkAnArray() {
    int[] values = { -64, 64, -8192, 8192, -1048576, 1048576 };
    byte[] packed = DlugoszVarLenIntPacker.pack(values);
    var decoder = new DlugoszVarLenIntPacker.Decoder(packed);
    int idx = 0;
    while (decoder.hasNext()) {
      assertEquals(values[idx++], decoder.next(), "value at index " + (idx - 1));
    }
    assertEquals(values.length, idx);
  }

  private void packTest(int[] arr, int expectedPackedLen) {
    byte[] packed = DlugoszVarLenIntPacker.pack(arr);
    assertEquals(
      expectedPackedLen,
      packed.length,
      "packed length for " + Arrays.toString(arr) + " = " + unsignedCharString(packed)
    );
    int[] unpacked = DlugoszVarLenIntPacker.unpack(packed);
    assertArrayEquals(arr, unpacked);
  }

  private String unsignedCharString(byte[] data) {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (int i = 0; i < data.length; i++) {
      sb.append(String.format("%02X", data[i] & 0xFF));
      if (i < data.length - 1) {
        sb.append(",");
      }
    }
    sb.append("]");
    return sb.toString();
  }
}
