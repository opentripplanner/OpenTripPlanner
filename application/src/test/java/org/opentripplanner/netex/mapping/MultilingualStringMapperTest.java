package org.opentripplanner.netex.mapping;

import static com.google.common.truth.Truth.assertThat;

import jakarta.xml.bind.annotation.adapters.NormalizedStringAdapter;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.MultilingualString;

class MultilingualStringMapperTest {

  private static final MultilingualString EMPTY = new MultilingualString();
  private static final MultilingualString BLANK = new MultilingualString().withContent("  ");
  private static final MultilingualString TEXT = new MultilingualString().withContent(
    "Line 1\r\n\tLine 2\n"
  );

  @Test
  void getStringValue() {
    assertThat(MultilingualStringMapper.getStringValue(null)).isNull();
    assertThat(MultilingualStringMapper.getStringValue(EMPTY)).isEmpty();
    assertThat(MultilingualStringMapper.getStringValue(BLANK)).isEqualTo("  ");
    assertThat(MultilingualStringMapper.getStringValue(TEXT)).isEqualTo("Line 1   Line 2 ");
  }

  @Test
  void nullableValueOf() {
    assertThat(MultilingualStringMapper.nullableValueOf(null)).isNull();
    assertThat(MultilingualStringMapper.nullableValueOf(EMPTY)).isNull();
    assertThat(MultilingualStringMapper.nullableValueOf(BLANK)).isNull();
    assertThat(MultilingualStringMapper.nullableValueOf(TEXT)).isEqualTo("Line 1   Line 2 ");
  }

  /**
   * Before NeTEx 2.0, MultilingualString#getValue() was normalized by JAXB's
   * NormalizedStringAdapter. Keep the exact same output.
   */
  @Test
  void sameWhitespaceNormalizationAsNetexModelBeforeVersion2() {
    var raw = " a\tb\r\nc\n  d\u00a0e\n";
    assertThat(
      MultilingualStringMapper.getStringValue(new MultilingualString().withContent(raw))
    ).isEqualTo(new NormalizedStringAdapter().unmarshal(raw));
  }
}
