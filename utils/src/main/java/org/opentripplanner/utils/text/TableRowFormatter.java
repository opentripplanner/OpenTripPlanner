package org.opentripplanner.framework.text;

import java.util.List;
import java.util.function.Function;
import org.opentripplanner.framework.lang.ObjectUtils;
import org.opentripplanner.framework.lang.StringUtils;

class TableRowFormatter {

  private static final String SEP = "|";
  private static final char AIR = ' ';
  private static final char LINE_CH = '-';
  private static final char ALIGN_CH = ':';
  private final List<Table.Align> alignment;
  private final List<Integer> widths;
  private boolean includeBoarder = false;
  private Function<String, String> escapeTextOp = s -> s;

  private TableRowFormatter(
    List<Table.Align> alignment,
    List<Integer> widths,
    boolean includeBoarder,
    Function<String, String> escapeTextOp
  ) {
    this.widths = widths;
    this.alignment = alignment;
    this.includeBoarder = includeBoarder;
    this.escapeTextOp = escapeTextOp;
  }

  static TableRowFormatter markdownFormatter(List<Table.Align> alignment, List<Integer> widths) {
    return new TableRowFormatter(alignment, widths, true, MarkdownFormatter::escapeInTable);
  }

  static TableRowFormatter logFormatter(List<Table.Align> alignment, List<Integer> widths) {
    return new TableRowFormatter(alignment, widths, false, it -> it);
  }

  String format(List<?> values) {
    var buf = new StringBuilder();
    for (int i = 0; i < values.size(); ++i) {
      if (i != 0) {
        buf.append(AIR).append(SEP).append(AIR);
      } else if (includeBoarder) {
        buf.append(SEP).append(AIR);
      }
      buf.append(normalizeText(values, i));
    }
    if (includeBoarder) {
      buf.append(AIR).append(SEP);
    }
    return buf.toString();
  }

  String markdownHeaderLine() {
    var buf = new StringBuilder(SEP);
    for (int i = 0; i < widths.size(); ++i) {
      var align = alignment.get(i);
      addAlignChar(buf, align == Table.Align.Center);
      StringUtils.append(buf, LINE_CH, widths.get(i));
      addAlignChar(buf, align == Table.Align.Center || align == Table.Align.Right);
      buf.append(SEP);
    }
    return buf.toString();
  }

  private void addAlignChar(StringBuilder buf, boolean add) {
    buf.append(add ? ALIGN_CH : LINE_CH);
  }

  private String normalizeText(List<?> row, int column) {
    var align = alignment.get(column);
    var width = widths.get(column);
    var text = ObjectUtils.toString(row.get(column));
    text = escapeTextOp.apply(text);
    text = align.pad(text, width);
    return text;
  }
}
