package org.opentripplanner.utils.text;

/**
 * Converts a byte array to its hexadecimal representation
 */
public class HexString {

  private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

  private HexString() {}

  public static String of(byte[] data) {
    // two characters form the hex value.
    StringBuilder out = new StringBuilder(data.length * 2);
    for (byte b : data) {
      out.append(HEX_CHARS[(0xF0 & b) >>> 4]);
      out.append(HEX_CHARS[0x0F & b]);
    }
    return out.toString();
  }
}
