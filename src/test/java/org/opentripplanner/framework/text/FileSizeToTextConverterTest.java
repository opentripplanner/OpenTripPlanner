package org.opentripplanner.framework.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.framework.text.FileSizeToTextConverter.fileSizeToString;

import org.junit.jupiter.api.Test;

public class FileSizeToTextConverterTest {

  @Test
  public void testFileSizeToString() {
    assertEquals("1 byte", fileSizeToString(1));
    assertEquals("12 bytes", fileSizeToString(12));
    assertEquals("123 bytes", fileSizeToString(123));
    assertEquals("1 kb", fileSizeToString(1234));
    assertEquals("12 kb", fileSizeToString(12345));
    assertEquals("123 kb", fileSizeToString(123456));
    assertEquals("1.2 MB", normalize(fileSizeToString(1234567)));
    assertEquals("12.3 MB", normalize(fileSizeToString(12345678)));
    // Round up
    assertEquals("123.5 MB", normalize(fileSizeToString(123456789)));
    assertEquals("1.2 GB", normalize(fileSizeToString(1234567890)));
    assertEquals("12.3 GB", normalize(fileSizeToString(12345678901L)));
  }

  private static String normalize(String number) {
    return number.replace(',', '.');
  }
}
