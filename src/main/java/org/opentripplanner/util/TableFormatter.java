package org.opentripplanner.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * This class is responsible for creating a pretty table that can be printed to a terminal window. Like this:
 * <pre>
 * Description  | Duration | Walk | Start time |  End  | Modes
 * Case one     |    34:13 | 1532 |      08:07 | 08:41 |  BUS
 * Case another |    26:00 |  453 |      08:29 | 08:55 |  BUS
 * </pre>
 */
public class TableFormatter {
    public enum Align {
        Left(TableFormatter::padLeft),
        Center(TableFormatter::padCenter),
        Right(TableFormatter::padRight);

        private final BiFunction<String, Integer, String> padFunction;

        private Align(BiFunction<String, Integer, String> padFunction) {
            this.padFunction = padFunction;
        }

        String pad(String value, int width) {
            return padFunction.apply(value, width);
        }
    }

    private final List<Align> aligns = new ArrayList<>();
    private final List<String> headers = new ArrayList<>();
    private final List<Integer> widths = new ArrayList<>();
    private final List<List<String>> rows = new ArrayList<>();


    /**
     * Use this constructor to create a table with headers and the use the
     * {@link #addRow(Object...)} to add rows to the table. The column
     * widths will be calculated based on the data in the table.
     */
    public TableFormatter(Collection<Align> aligns, Collection<String> headers) {
        if (aligns.size() != headers.size()) {
            throw new IllegalArgumentException();
        }
        this.aligns.addAll(aligns);
        this.headers.addAll(headers);
        this.headers.forEach(it -> widths.add(it.length()));
    }

    /**
     * Use this constructor to create a table with headers and fixed width columns.
     * This is usful if you want to print header and row during computation and can not hold
     * the entire table in memory until all values are added.
     * <p>
     * Use the {@code print} methods to return each line.
     */
    public TableFormatter(Collection<Align> aligns, Collection<String> headers, int ... widths) {
        if (aligns.size() != headers.size() || aligns.size() != widths.length) {
            throw new IllegalArgumentException();
        }
        this.aligns.addAll(aligns);
        this.headers.addAll(headers);
        for (int i = 0; i < widths.length; i++) {
            int width = Math.max(this.headers.get(i).length(), widths[i]);
            this.widths.add(width);
        }
    }

    public String printHeader() {
        StringBuilder buf = new StringBuilder();
        appendRow(buf, headers, this::padHeader);
        return buf.toString();
    }

    public String printRow(Object... row) {
        StringBuilder buf = new StringBuilder();
        appendRow(buf, toStrings(row), this::pad);
        return buf.toString();
    }

    public void addRow(Object... row) {
        addRow(Arrays.stream(row).collect(Collectors.toList()));
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        appendRowLn(buf, headers, this::padHeader);

        for (List<String> row : rows) {
            appendRowLn(buf, row, this::pad);
        }
        return buf.toString();
    }


    /* private methods */

    private void addRow(Collection<?> row) {
        assertRowIsLessThanOrSameSizeAsHeader(row);
        List<String> aRow = toStrings(row);
        setColumnWidths(aRow);
        rows.add(aRow);
    }

    private void appendRow(StringBuilder buf, List<String> row, BiFunction<List<String>, Integer, String> pad) {
        for (int i = 0; i < row.size(); ++i) {
            if(i!=0) { buf.append(" | "); }
            buf.append(pad.apply(row, i));
        }
    }

    private void appendRowLn(StringBuilder buf, List<String> row, BiFunction<List<String>, Integer, String> pad) {
        appendRow(buf, row, pad);
        buf.append('\n');
    }

    private String pad(List<String> row, int i) {
        return aligns.get(i).pad(row.get(i), widths.get(i));
    }

    private String padHeader(List<String> row, int i) {
        return headerAlignment(i).pad(row.get(i), widths.get(i));
    }

    private Align headerAlignment(int i) {
        return aligns.get(i) == Align.Left ? Align.Left : Align.Center;
    }

    private void setColumnWidths(List<String> row) {
        for (int i = 0; i < row.size(); ++i) {
            widths.set(i, Math.max(widths.get(i), row.get(i).length()));
        }
    }

    private static List<String> toStrings(Collection<?> row) {
        return row.stream().map(it -> it == null ? "" : it.toString()).collect(Collectors.toList());
    }

    private static List<String> toStrings(Object ... row) {
        return Arrays.stream(row).map(it -> it == null ? "" : it.toString()).collect(Collectors.toList());
    }

    private void assertRowIsLessThanOrSameSizeAsHeader(Collection<?> row) {
        if (row.size() > headers.size()) {
            throw new IllegalArgumentException(
                    "Can not add row with more columns than the header. " +
                            "Row size: " + row.size() + ", Header size: " + headers.size()
            );
        }
    }

    private static String padLeft(String value, int width) {
        return pad(value, width, (b) -> false);
    }

    private static String padRight(String value, int width) {
        return pad(value, width, (b) -> true);
    }

    private static String padCenter(String value, int width) {
        return pad(value, width, (b) -> !b);
    }

    private static String pad(String value, int width, Function<Boolean, Boolean> toRight) {
        boolean toggle = toRight.apply(false);
        StringBuilder buf = new StringBuilder(value);
        while (buf.length() < width) {
            if (toggle) {
                buf.insert(0, ' ');
            } else {
                buf.append(' ');
            }
            toggle = toRight.apply(toggle);
        }
        value = buf.toString();
        return value;
    }
}
