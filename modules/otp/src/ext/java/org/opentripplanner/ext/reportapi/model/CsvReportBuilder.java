package org.opentripplanner.ext.reportapi.model;

import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.framework.time.TimeUtils;

/**
 * A very simple CSV builder to create CSV reports.
 * <p>
 * This class helps to format common types like time, duration and enums.
 */
class CsvReportBuilder {

  public static final char NEW_LINE = '\n';
  private final String sep;
  private final StringBuilder buf = new StringBuilder();

  CsvReportBuilder() {
    this(";");
  }

  CsvReportBuilder(String separator) {
    sep = separator;
  }

  @Override
  public String toString() {
    return buf.toString();
  }

  void addEnum(Enum<?> enumValue) {
    buf.append(enumValue);
    sep();
  }

  void addDuration(int duration, int notSet) {
    buf.append(DurationUtils.durationToStr(duration, notSet));
    sep();
  }

  void addTime(int time, int notSet) {
    buf.append(TimeUtils.timeToStrLong(time, notSet));
    sep();
  }

  /** Add the column headers including new line. */
  void addHeader(String... headers) {
    for (String it : headers) {
      addText(it);
    }
    newLine();
  }

  void addText(String text) {
    buf.append(text);
    sep();
  }

  void addNumber(Number num) {
    buf.append(num == null ? "" : num.toString());
    sep();
  }

  void addBoolean(Boolean b) {
    buf.append(b == null ? "" : b.toString());
    sep();
  }

  void addOptText(boolean addIt, String text) {
    if (addIt) {
      buf.append(text);
    }
    sep();
  }

  void sep() {
    buf.append(sep);
  }

  void newLine() {
    buf.append(NEW_LINE);
  }
}
