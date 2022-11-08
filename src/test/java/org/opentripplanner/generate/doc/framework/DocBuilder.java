package org.opentripplanner.generate.doc.framework;

import static org.opentripplanner.framework.text.MarkdownFormatter.NEW_LINE;

import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.framework.text.MarkdownFormatter;
import org.opentripplanner.standalone.config.framework.json.EnumMapper;

/**
 * Builder for creating a new document
 */
@SuppressWarnings("UnusedReturnValue")
public class DocBuilder {

  private final StringBuilder buffer = new StringBuilder();

  /**
   * Add new line to output document - note this do not break the line in the generated HTML
   * end result.
   */
  public DocBuilder newLine() {
    buffer.append(NEW_LINE);
    return this;
  }

  /**
   * Break the line in the target document. Ths adds a {@code " \"} and a new-line to the buffer.
   */
  public DocBuilder lineBreak() {
    buffer.append(MarkdownFormatter.lineBreak());
    return newLine();
  }

  /**
   * New paragraphs are started after two new-lines in Markdown. This adds to new-lines.
   */
  public DocBuilder endParagraph() {
    return newLine().newLine();
  }

  /**
   * Add header with the appropriate new-lines after.
   */
  public DocBuilder header(int level, String header, String anchor) {
    return addSection(MarkdownFormatter.header(level, header, anchor));
  }

  /**
   * Add a label to the document followed by a SPACE.
   */
  public DocBuilder label(String label) {
    buffer.append(MarkdownFormatter.bold(label)).append(ParameterDetailsList.SPACE);
    return this;
  }

  /**
   * Add the text as inline code fragment followed by a SPACE.
   */
  public DocBuilder code(Object code) {
    buffer.append(MarkdownFormatter.code(code)).append(ParameterDetailsList.SPACE);
    return this;
  }

  /**
   * Add the text as inline code fragment followed by a SPACE.
   */
  public DocBuilder path(String path) {
    buffer.append(path).append(ParameterDetailsList.SPACE);
    return this;
  }

  /**
   * Add plain text to document followed by a SPACE.
   */
  public DocBuilder text(Object value) {
    buffer.append(value).append(ParameterDetailsList.SPACE);
    return this;
  }

  /**
   * Add a separator to separate elements on one line followed by a SPACE. This creates some air
   * between each element.
   */
  public DocBuilder dotSeparator() {
    buffer.append("∙ ");
    return this;
  }

  /**
   * Add text as a section in the document followed by two LINE_BREAKS (end of paragraph).
   */
  public DocBuilder addSection(String text) {
    if (text == null || text.isBlank()) {
      return this;
    }
    buffer.append(text);
    return endParagraph();
  }

  public void addExample(String comment, String body) {
    buffer.append(
      """
      ```JSON
      // %s
      {
        %s
      }
      ```
      """.formatted(
          comment,
          body.indent(2)
        )
    );
  }

  /**
   * Add a list of enum values to document
   */
  DocBuilder addEnums(List<? extends Enum<?>> enums) {
    buffer.append(
      enums
        .stream()
        .map(EnumMapper::toString)
        .map(MarkdownFormatter::code)
        .collect(Collectors.joining(" | "))
    );
    return this;
  }

  String toDoc() {
    return buffer.toString();
  }

  @Override
  public String toString() {
    return buffer.toString();
  }
}
