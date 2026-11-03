package org.opentripplanner.netex.mapping;

import jakarta.xml.bind.JAXBElement;
import java.io.Serializable;
import java.util.List;
import org.entur.netex2.model.MultilingualString;
import org.entur.netex2.model.TextType;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.utils.lang.StringUtils;

public class MultilingualStringMapper {

  @Nullable
  public static String nullableValueOf(@Nullable MultilingualString multilingualString) {
    if (multilingualString == null) {
      return null;
    }

    String value = getStringValue(multilingualString);
    if (StringUtils.hasNoValue(value)) {
      return null;
    }

    return value;
  }

  /**
   * Extract the string value from a MultilingualString.
   * In NeTEx 2.0, MultilingualString uses a mixed content model where the text is stored in
   * getContent() as a list of plain text nodes and optional nested {@code <Text lang="..">}
   * elements holding alternative translations. The plain text is used when present; otherwise
   * the first non-blank nested {@code <Text>} element is used.
   * <p>
   * Returns {@code null} only if the element is absent. An empty element (e.g.
   * {@code <Description/>}) returns an empty string, like {@code getValue()} did before NeTEx 2.0.
   * Use {@link #nullableValueOf(MultilingualString)} to map empty values to {@code null}.
   * <p>
   * Tabs, carriage returns and line feeds are each replaced by a space ({@code xs:normalizedString}
   * semantics), like the {@code NormalizedStringAdapter} applied to {@code getValue()} before
   * NeTEx 2.0. Text is neither trimmed nor collapsed. A text which needs no normalization is
   * returned as the same instance JAXB unmarshalled, so that equal strings stay shared.
   */
  @Nullable
  public static String getStringValue(@Nullable MultilingualString multilingualString) {
    if (multilingualString == null) {
      return null;
    }
    List<Serializable> content = multilingualString.getContent();

    // Fast path for the common case: a single plain text node
    if (content.size() == 1 && content.getFirst() instanceof String s) {
      return normalize(s);
    }

    StringBuilder text = new StringBuilder();
    String firstTextElement = null;
    for (Serializable item : content) {
      if (item instanceof String s) {
        text.append(s);
      } else if (firstTextElement == null && item instanceof JAXBElement<?> element) {
        firstTextElement = nonBlankTextElementValue(element);
      }
    }
    if (firstTextElement != null && StringUtils.hasNoValue(text.toString())) {
      return normalize(firstTextElement);
    }
    return normalize(text.toString());
  }

  @Nullable
  private static String nonBlankTextElementValue(JAXBElement<?> element) {
    if (
      element.getValue() instanceof TextType textType && StringUtils.hasValue(textType.getValue())
    ) {
      return textType.getValue();
    }
    return null;
  }

  private static String normalize(String text) {
    int i = indexOfWhitespaceToNormalize(text);
    if (i < 0) {
      return text;
    }
    char[] chars = text.toCharArray();
    for (; i < chars.length; i++) {
      if (isWhitespaceToNormalize(chars[i])) {
        chars[i] = ' ';
      }
    }
    return new String(chars);
  }

  private static int indexOfWhitespaceToNormalize(String text) {
    for (int i = 0; i < text.length(); i++) {
      if (isWhitespaceToNormalize(text.charAt(i))) {
        return i;
      }
    }
    return -1;
  }

  private static boolean isWhitespaceToNormalize(char c) {
    return c == '\t' || c == '\n' || c == '\r';
  }
}
