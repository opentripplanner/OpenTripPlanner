package org.opentripplanner.ext.reportapi.model;

import org.opentripplanner.util.time.DurationUtils;
import org.opentripplanner.util.time.TimeUtils;

/**
 * A very simple CSV builder to create CSV reports.
 * <p>
 * This class helps formatting common types like time, duration and enums.
 */
class CsvReportBuilder {
    private static final String SEP = ";";
    public static final char NEW_LINE = '\n';


    private final StringBuilder buf = new StringBuilder();

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
    void addHeader(String ... headers) {
        for (String it : headers) {
            addText(it);
        }
        newLine();
    }

    void addText(String text) {
        buf.append(text);
        sep();

    }

    void addOptText(boolean addIt, String text) {
        if(addIt) { buf.append( text); }
        sep();
    }

    void sep() {
        buf.append(SEP);
    }

    void newLine() {
        buf.append(NEW_LINE);
    }

    @Override
    public String toString() {
        return buf.toString();
    }
}
