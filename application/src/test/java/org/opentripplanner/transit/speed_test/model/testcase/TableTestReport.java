package org.opentripplanner.transit.speed_test.model.testcase;

import static org.opentripplanner.utils.text.Table.Align.Center;
import static org.opentripplanner.utils.text.Table.Align.Left;
import static org.opentripplanner.utils.text.Table.Align.Right;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.routing.util.DiffEntry;
import org.opentripplanner.utils.text.Table;
import org.opentripplanner.utils.text.TableBuilder;
import org.opentripplanner.utils.time.TimeUtils;

/**
 * This class is responsible for creating a test report as a table. The Table is easy to read and
 * can be printed to a terminal window.
 */
public class TableTestReport {

  public static String report(List<DiffEntry<Result>> results) {
    if (results.isEmpty()) {
      return "NO RESULTS FOUND FOR TEST CASE!";
    }

    TableBuilder table = newTable();

    for (DiffEntry<Result> it : results) {
      addTo(table, it);
    }
    return table.toString();
  }

  /* private methods */

  private static TableBuilder newTable() {
    return Table.of()
      .withAlights(Center, Right, Right, Right, Right, Right, Center, Center, Left, Left, Left)
      .withHeaders(
        "STATUS",
        "TX",
        "Duration",
        "Cost",
        "Start",
        "End",
        "Modes",
        "Agencies",
        "Routes",
        "Stops",
        "Legs"
      );
  }

  private static void addTo(TableBuilder table, DiffEntry<Result> e) {
    Result result = e.element();
    table.addRow(
      e.status(TestStatus.OK.label, TestStatus.FAILED.label, TestStatus.WARN.label),
      result.nTransfers(),
      result.durationAsStr(),
      result.cost(),
      TimeUtils.timeToStrLong(result.startTime()),
      TimeUtils.timeToStrLong(result.endTime()),
      toStr(result.modes()),
      toStr(result.agencies()),
      toStr(result.routes()),
      toStr(result.stops()),
      result.details()
    );
  }

  private static String intsToStr(Collection<Integer> list) {
    return list.isEmpty()
      ? "-"
      : list.stream().map(Object::toString).collect(Collectors.joining(" "));
  }

  private static String toStr(Collection<?> list) {
    return list.isEmpty()
      ? "-"
      : list
        .stream()
        .peek(s -> {
          if (s == null) {
            throw new IllegalArgumentException("null value in list " + list);
          }
        })
        .map(Object::toString)
        .collect(Collectors.joining(" "));
  }
}
