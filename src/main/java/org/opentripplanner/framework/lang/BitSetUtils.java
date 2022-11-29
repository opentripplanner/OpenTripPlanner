package org.opentripplanner.framework.lang;

/**
 * A compact bit set utility class. It rely on the client to store the bit set himself (either as a
 * byte, short, int or long, depending on the amount of bits needed), to prevent having to store a
 * reference and create another heavy object instance such as with the BitSet class.
 * <p>
 * Usage is rather simple:
 *
 * <pre>
 *
 * short flags; // or int, long, byte
 *
 * public boolean isFooBar() {
 *     return BitSetUtils.get(flags, FOOBAR_INDEX);
 * }
 *
 * public void setFooBar(boolean foobar) {
 *     flags = BitSetUtils.set(flags, FOOBAR_INDEX, foobar);
 * }
 *
 * </pre>
 *
 * @author laurent
 */
public final class BitSetUtils {

  public static boolean get(byte bitset, int index) {
    return (bitset & (1 << index)) != 0;
  }

  public static byte set(byte bitset, int index, boolean value) {
    if (value) {
      bitset |= (1 << index);
    } else {
      bitset &= ~(1 << index);
    }
    return bitset;
  }

  public static boolean get(short bitset, int index) {
    return (bitset & (1 << index)) != 0;
  }

  public static short set(short bitset, int index, boolean value) {
    if (value) {
      bitset |= (1 << index);
    } else {
      bitset &= ~(1 << index);
    }
    return bitset;
  }

  public static boolean get(int bitset, int index) {
    return (bitset & (1 << index)) != 0;
  }

  public static int set(int bitset, int index, boolean value) {
    if (value) {
      bitset |= (1 << index);
    } else {
      bitset &= ~(1 << index);
    }
    return bitset;
  }

  public static boolean get(long bitset, int index) {
    return (bitset & (1L << index)) != 0;
  }

  public static long set(long bitset, int index, boolean value) {
    if (value) {
      bitset |= (1L << index);
    } else {
      bitset &= ~(1L << index);
    }
    return bitset;
  }
}
