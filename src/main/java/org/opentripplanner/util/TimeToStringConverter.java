package org.opentripplanner.util;

import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeToStringConverter {
    private static final Pattern TIME_PATTERN = Pattern.compile("^(-?)(\\d+):(\\d\\d):(\\d\\d)$");

    private static final DecimalFormat TWO_DIGET_FORMAT = new DecimalFormat("00");

    public static String toHH_MM_SS(final int seconds) {
        int absSeconds = Math.abs(seconds);

        int secondsRest = absSeconds % 60;
        int hourAndMinutes = (absSeconds - secondsRest) / 60;
        int minutes = hourAndMinutes % 60;
        int hours = (hourAndMinutes - minutes) / 60;

        StringBuilder b = new StringBuilder();

        if (seconds < 0) {
            b.append('-');
        }
        b.append(TWO_DIGET_FORMAT.format(hours));
        b.append(":");
        b.append(TWO_DIGET_FORMAT.format(minutes));
        b.append(":");
        b.append(TWO_DIGET_FORMAT.format(secondsRest));
        return b.toString();
    }

    public static int parseHH_MM_SS(String value) {
        Matcher m = TIME_PATTERN.matcher(value);
        if (!m.matches())
            throw new InvalidTimeException(value, TIME_PATTERN.pattern());
        try {
            int sign = "-".equals(m.group(1)) ? -1 : 1;
            int hours = Integer.parseInt(m.group(2));
            int minutes = Integer.parseInt(m.group(3));
            int seconds = Integer.parseInt(m.group(4));

            return sign * (60 * (60 * hours + minutes) + seconds);
        } catch (NumberFormatException ex) {
            throw new InvalidTimeException(value, TIME_PATTERN.pattern());
        }
    }
}
