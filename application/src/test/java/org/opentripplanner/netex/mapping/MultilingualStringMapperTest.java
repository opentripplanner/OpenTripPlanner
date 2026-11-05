package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.adapters.NormalizedStringAdapter;
import org.entur.netex2.model.MultilingualString;
import org.entur.netex2.model.ObjectFactory;
import org.entur.netex2.model.TextType;
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

  @Test
  void plainTextIsReturnedAsIsWhenNoNormalizationIsNeeded() {
    String text = "Oslo S";
    assertSame(
      text,
      MultilingualStringMapper.getStringValue(new MultilingualString().withContent(text))
    );
  }

  @Test
  void textNodesAreConcatenated() {
    var value = new MultilingualString().withContent("Oslo", " ", "S");
    assertEquals("Oslo S", MultilingualStringMapper.getStringValue(value));
  }

  @Test
  void nestedTextElementIsUsedWhenThereIsNoPlainText() {
    // <Name>\n  <Text lang="en">Oslo Central</Text>\n</Name>
    var value = new MultilingualString().withContent("\n  ", text("Oslo Central"), "\n");
    assertEquals("Oslo Central", MultilingualStringMapper.getStringValue(value));
    assertEquals("Oslo Central", MultilingualStringMapper.nullableValueOf(value));
  }

  @Test
  void firstNonBlankNestedTextElementIsUsed() {
    var value = new MultilingualString().withContent(text(" "), text("Oslo\tS"), text("Oslo S"));
    assertEquals("Oslo S", MultilingualStringMapper.getStringValue(value));
  }

  @Test
  void plainTextTakesPrecedenceOverNestedTextElements() {
    var value = new MultilingualString().withContent("Oslo S", text("Oslo Central"));
    assertEquals("Oslo S", MultilingualStringMapper.getStringValue(value));
  }

  @Test
  void blankNestedTextElementsOnlyGiveAnEmptyString() {
    var value = new MultilingualString().withContent(text(" "), text(null));
    assertEquals("", MultilingualStringMapper.getStringValue(value));
    assertNull(MultilingualStringMapper.nullableValueOf(value));
  }

  private static JAXBElement<TextType> text(String value) {
    return new ObjectFactory().createMultilingualStringText(new TextType().withValue(value));
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
