package org.opentripplanner.transit.raptor.speed_test.testcase;

import org.opentripplanner.transit.raptor.util.TimeUtils;
import org.opentripplanner.util.TableFormatter;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.opentripplanner.util.TableFormatter.Align.Center;
import static org.opentripplanner.util.TableFormatter.Align.Left;
import static org.opentripplanner.util.TableFormatter.Align.Right;


/**
 * This class is responsible for creating a test report as a table.
 * The Table is easy to read and can be printed to a terminal window.
 */
public class TableTestReport {

    public static String report(List<Result> results) {
        if (results.isEmpty()) {
            return "NO RESULTS FOUND FOR TEST CASE!";
        }

        Collections.sort(results);

        TableFormatter table = newTable();
        for (Result it : results) {
            addTo(table, it);
        }
        return table.toString();
    }


    /* private methods */

    private static TableFormatter newTable() {
        return new TableFormatter(
            List.of(Center, Right, Right, Right, Right, Right, Center, Center, Left, Left, Left),
            List.of("STATUS", "TF", "Duration", "Cost",  "Start", "End", "Modes", "Agencies", "Routes", "Stops", "Legs")
        );
    }

    private static void addTo(TableFormatter table, Result result) {
        table.addRow(
            result.status.label,
            result.transfers,
            result.durationAsStr(),
            result.cost,
            TimeUtils.timeToStrLong(result.startTime),
            TimeUtils.timeToStrLong(result.endTime),
            toStr(result.modes),
            toStr(result.agencies),
            toStr(result.routes),
            intsToStr(result.stops),
            result.details
        );
    }

    private static String intsToStr(Collection<Integer> list) {
        return list.isEmpty() ? "-" : list.stream().map(Object::toString).collect(Collectors.joining(" "));
    }

    private static String toStr(Collection<?> list) {
        return list.isEmpty()
                ? "-"
                : list.stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(" "));
    }
}
