package org.opentripplanner.street.geometry;

import java.io.ByteArrayOutputStream;

/**
 * Variable-length integer encoding. This optimize integer storage when most of the values are
 * small, but few of them can be quite large (as in a geometry). Adapted Dlugosz scheme to support
 * signed int whose average are around 0.
 * <p>
 * See Dlugosz' variable-length integer encoding (http://www.dlugosz.com/ZIP2/VLI.html).
 *
 * @author laurent
 */
class DlugoszVarLenIntPacker {

  static byte[] pack(int[] arr) {
    if (arr == null) {
      return null;
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream(arr.length);
    for (int i : arr) {
      if (i >= -64 && i <= 63) {
        // 0xxx xxxx -> 7 bits value
        // i+64 between 0 and 127, 7 bits
        int ui = i + 64;
        baos.write(ui);
      } else if (i >= -8192 && i <= 8191) {
        // 10xx xxxx + 8 -> 14 bits value
        // i+8192 between 0 and 16383
        int ui = i + 8192;
        // 6b MSB
        baos.write(0x80 | (ui >> 8));
        // 8b LSB
        baos.write(ui & 0xFF);
      } else if (i >= -1048576 && i <= 1048575) {
        // 110 xxxx + 2x8 -> 21 bits value
        // i + 1048576 between 0 and 2097151
        int ui = i + 1048576;
        // 5b MSB
        baos.write(0xC0 | (ui >> 16));
        // 8b
        baos.write((ui >> 8) & 0xFF);
        // 8b
        baos.write(ui & 0xFF);
      } else if (i >= -67108864 && i <= 67108863) {
        // 1110 0xxx + 3x8 -> 27 bits value
        // i + 67108864 between 0 and 134217727
        int ui = i + 67108864;
        // 3b MSB
        baos.write(0xE0 | (ui >> 24));
        // 8b
        baos.write((ui >> 16) & 0xFF);
        // 8b
        baos.write((ui >> 8) & 0xFF);
        // 8b
        baos.write(ui & 0xFF);
      } else {
        // int can't have more than 32 bits
        // 1110 1xxx + 4x8 -> 35 bits value
        // i + 0x80000000 fits in 35 bits for sure
        long ui = (long) i + 2147483648L;
        // 3b MSB
        baos.write((int) (0xE8 | (ui >> 32)));
        // 8b
        baos.write((int) ((ui >> 24) & 0xFF));
        // 8b
        baos.write((int) ((ui >> 16) & 0xFF));
        // 8b
        baos.write((int) ((ui >> 8) & 0xFF));
        // 8b
        baos.write((int) (ui & 0xFF));
      }
    }
    return baos.toByteArray();
  }

  static int[] unpack(byte[] arr) {
    if (arr == null) {
      return null;
    }
    int[] out = new int[countValues(arr)];
    var decoder = new Decoder(arr);
    int idx = 0;
    while (decoder.hasNext()) {
      out[idx++] = decoder.next();
    }
    return out;
  }

  /**
   * Number of ints encoded in {@code arr} without decoding their values. Walks the leading tag of
   * each varint to skip its body. Used by allocation-free decoders to size their target buffers.
   */
  static int countValues(byte[] arr) {
    if (arr == null) {
      return 0;
    }
    int n = 0;
    int pos = 0;
    while (pos < arr.length) {
      int v1 = arr[pos] & 0xFF;
      if ((v1 & 0x80) == 0x00) {
        pos += 1;
      } else if ((v1 & 0xC0) == 0x80) {
        pos += 2;
      } else if ((v1 & 0xE0) == 0xC0) {
        pos += 3;
      } else if ((v1 & 0xF8) == 0xE0) {
        pos += 4;
      } else {
        pos += 5;
      }
      n++;
    }
    return n;
  }

  /**
   * Stateful cursor over a packed byte array. It holds the array and the current read position and
   * decodes one varint per {@link #next()} call, advancing the position. This keeps the cursor
   * bookkeeping out of the call sites: callers loop on {@link #hasNext()} and treat {@link #next()}
   * as a plain {@code int} source, accumulating deltas themselves where coordinates are involved.
   */
  static final class Decoder {

    private final byte[] arr;
    private int pos = 0;

    Decoder(byte[] arr) {
      this.arr = arr;
    }

    boolean hasNext() {
      return pos < arr.length;
    }

    /**
     * Decode the varint at the current position, advance past it, and return its signed value.
     * <p>
     * Implementation note: This method is hot and is currently short enough to be inlined
     * by the JIT. When changing the implementation or refactoring, make sure to measure
     * performance with a microbenchmark.
     */
    int next() {
      int p = pos;
      int v1 = arr[p] & 0xFF;
      int sv;
      int width;
      if ((v1 & 0x80) == 0x00) {
        // 0xxx xxxx -> 7 bits value
        sv = (v1 & 0x7F) - 64;
        width = 1;
      } else if ((v1 & 0xC0) == 0x80) {
        // 10xx xxxx + 8 -> 14 bits value
        sv = ((v1 & 0x3F) << 8) + (arr[p + 1] & 0xFF) - 8192;
        width = 2;
      } else if ((v1 & 0xE0) == 0xC0) {
        // 110 xxxx + 2x8 -> 21 bits value
        sv = ((v1 & 0x1F) << 16) + ((arr[p + 1] & 0xFF) << 8) + (arr[p + 2] & 0xFF) - 1048576;
        width = 3;
      } else if ((v1 & 0xF8) == 0xE0) {
        // 1110 0xxx + 3x8 -> 27 bits value
        sv =
          ((v1 & 0x1F) << 24) +
          ((arr[p + 1] & 0xFF) << 16) +
          ((arr[p + 2] & 0xFF) << 8) +
          (arr[p + 3] & 0xFF) -
          67108864;
        width = 4;
      } else {
        // 1110 1xxx + 4x8 -> 35 bits value; the tag occupies the upper 5 bits,
        // leaving 3 value bits in v1.
        long lsv =
          (((long) v1 & 0x07) << 32) +
          ((arr[p + 1] & 0xFF) << 24) +
          ((arr[p + 2] & 0xFF) << 16) +
          ((arr[p + 3] & 0xFF) << 8) +
          (arr[p + 4] & 0xFF) -
          2147483648L;
        sv = (int) lsv;
        width = 5;
      }
      pos = p + width;
      return sv;
    }
  }
}
