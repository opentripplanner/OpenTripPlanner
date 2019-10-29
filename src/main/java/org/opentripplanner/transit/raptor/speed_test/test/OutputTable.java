package org.opentripplanner.transit.raptor.speed_test.test;

import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;


/**
 * This class is responsible for creating a pretty table that can be printed to a terminal window. Like this:
 * <pre>
 * Description  | Duration | Walk | Start time |  End  | Modes
 * Case one     |    34:13 | 1532 |      08:07 | 08:41 |  BUS
 * Case another |    26:00 |  453 |      08:29 | 08:55 |  BUS
 * </pre>
 */
class OutputTable {
    public enum Align {
        Left {
            @Override
            String pad(String value, int width) {
                return Strings.padEnd(value, width, ' ');
            }
        },
        Center {
            @Override
            String pad(String value, int width) {
                boolean toggle = true;
                StringBuilder buf = new StringBuilder(value);
                while (buf.length() < width) {
                    if (toggle) {
                        buf.insert(0, ' ');
                    } else {
                        buf.append(' ');
                    }
                    toggle = !toggle;
                }
                value = buf.toString();
                return value;
            }
        },
        Right {
            @Override
            String pad(String value, int width) {
                return Strings.padStart(value, width, ' ');
            }
        };

        abstract String pad(String value, int width);
    }

    private List<Align> aligns = new ArrayList<>();
    private List<String> headers = new ArrayList<>();
    private List<Integer> widths = new ArrayList<>();
    private List<List<String>> rows = new ArrayList<>();


    OutputTable(Collection<Align> aligns, Collection<String> headers) {
        if (aligns.size() != headers.size()) {
            throw new IllegalArgumentException();
        }
        this.aligns.addAll(aligns);
        this.headers.addAll(headers);
        this.headers.forEach(it -> widths.add(it.length()));
    }

    void addRow(Object... row) {
        addRow(Arrays.stream(row).collect(Collectors.toList()));
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        appendRow(buf, headers, this::padHeader);

        for (List<String> row : rows) {
            appendRow(buf, row, this::pad);
        }
        return buf.toString();
    }


    /* private methods */

    private void addRow(Collection<Object> row) {
        assertRowIsLessThanOrSameSizeAsHeader(row);
        List<String> aRow = row.stream().map(it -> it == null ? "" : it.toString()).collect(Collectors.toList());
        setColumnWidths(aRow);
        rows.add(aRow);
    }

    private void appendRow(StringBuilder buf, List<String> row, BiFunction<List<String>, Integer, String> pad) {
        for (int i = 0; i < row.size(); ++i) {
            if(i>0)  buf.append(" | ");
            buf.append(pad.apply(row, i));
        }
        buf.append('\n');
    }

    private String pad(List<String> row, int i) {
        return padAt(aligns.get(i), row, i);
    }

    private String padHeader(List<String> row, int i) {
        return padAt(headerAlignment(i), row, i);
    }

    private Align headerAlignment(int i) {
        return aligns.get(i) == Align.Left ? Align.Left : Align.Center;
    }

    private String padAt(Align align, List<String> row, int i) {
        return align.pad(row.get(i), widths.get(i));
    }

    private void setColumnWidths(List<String> row) {
        for (int i = 0; i < row.size(); ++i) {
            widths.set(i, Math.max(widths.get(i), row.get(i).length()));
        }
    }

    private void assertRowIsLessThanOrSameSizeAsHeader(Collection<Object> row) {
        if (row.size() > headers.size()) {
            throw new IllegalArgumentException(
                    "Can not add row with more columns than the header. " +
                            "Row size: " + row.size() + ", Header size: " + headers.size()
            );
        }
    }
}
