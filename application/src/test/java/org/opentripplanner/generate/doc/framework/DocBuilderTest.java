package org.opentripplanner.generate.doc.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.utils.text.MarkdownFormatter.HEADER_1;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class DocBuilderTest {

  DocBuilder subject = new DocBuilder()
    .addSection("Section")
    .label("Label")
    .code("code")
    .dotSeparator()
    .text("Text")
    .lineBreak()
    .text("More text")
    .endParagraph()
    .header(HEADER_1, "Header", "anchor")
    .addSection("Paragraph")
    .label("Enums")
    .addEnums(Arrays.stream(Foo.values()).toList())
    .newLine();

  @Test
  void toDoc() {
    assertEquals(
      """
      Section

      **Label** `code` ∙ Text\s\s\s
      More text\s

      <h1 id="anchor">Header</h1>

      Paragraph

      **Enums** `a` | `bar` | `boo-boo`
      """,
      subject.toDoc()
    );
  }

  @Test
  void toStringTest() {
    assertEquals(subject.toDoc(), subject.toString());
  }

  enum Foo {
    A,
    BAR,
    BOO_BOO,
  }
}
