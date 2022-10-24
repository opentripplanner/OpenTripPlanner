package org.opentripplanner.framework.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MarkdownFormatterTest {

  @Test
  void em() {
    assertEquals("*text*", MarkdownFormatter.em("text"));
  }

  @Test
  void bold() {
    assertEquals("**text**", MarkdownFormatter.bold("text"));
  }

  @Test
  void code() {
    assertEquals("`text`", MarkdownFormatter.code("text"));
  }

  @Test
  void quote() {
    assertEquals("\"text\"", MarkdownFormatter.quote("text"));
    assertEquals("", MarkdownFormatter.quote(null));
  }

  @Test
  void linkToAnchor() {
    assertEquals("[text](#anchor)", MarkdownFormatter.linkToAnchor("text", "anchor"));
  }

  @Test
  void linkToDoc() {
    assertEquals("[text](doc)", MarkdownFormatter.linkToDoc("text", "doc"));
  }

  @Test
  void checkMark() {
    assertEquals("✓️", MarkdownFormatter.checkMark(true));
  }

  @Test
  void escapeInTable() {
    assertEquals(
      "Text with pipe '¦' in it!",
      MarkdownFormatter.escapeInTable("Text with pipe '|' in it!")
    );
  }

  @Test
  void indentInTable() {
    assertEquals("\u00A0\u00A0\u00A0\u00A0", MarkdownFormatter.indentInTable());
  }
}
