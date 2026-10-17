package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.xml.bind.annotation.adapters.NormalizedStringAdapter;
import org.entur.netex2.model.MultilingualString;
import org.junit.jupiter.api.Test;

class MultilingualStringMapperTest {

  private static final MultilingualString EMPTY = new MultilingualString();
  private static final MultilingualString BLANK = new MultilingualString().withContent("  ");
  private static final MultilingualString TEXT = new MultilingualString().withContent(
    "Line 1\r\n\tLine 2\n"
  );

  @Test
  void getStringValue() {
    assertNull(MultilingualStringMapper.getStringValue(null));
    assertEquals("", MultilingualStringMapper.getStringValue(EMPTY));
    assertEquals("  ", MultilingualStringMapper.getStringValue(BLANK));
    assertEquals("Line 1   Line 2 ", MultilingualStringMapper.getStringValue(TEXT));
  }

  @Test
  void nullableValueOf() {
    assertNull(MultilingualStringMapper.nullableValueOf(null));
    assertNull(MultilingualStringMapper.nullableValueOf(EMPTY));
    assertNull(MultilingualStringMapper.nullableValueOf(BLANK));
    assertEquals("Line 1   Line 2 ", MultilingualStringMapper.nullableValueOf(TEXT));
  }

  /**
   * Before NeTEx 2.0, MultilingualString#getValue() was normalized by JAXB's
   * NormalizedStringAdapter. Keep the exact same output.
   */
  @Test
  void sameWhitespaceNormalizationAsNetexModelBeforeVersion2() {
    var raw = " a\tb\r\nc\n  d\u00a0e\n";
    assertEquals(
      new NormalizedStringAdapter().unmarshal(raw),
      MultilingualStringMapper.getStringValue(new MultilingualString().withContent(raw))
    );
  }
}
