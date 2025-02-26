package org.opentripplanner.routing.algorithm.raptoradapter.path;

import static org.opentripplanner.utils.text.Table.Align.Center;
import static org.opentripplanner.utils.text.Table.Align.Left;
import static org.opentripplanner.utils.text.Table.Align.Right;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.PathLeg;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.routing.util.DiffEntry;
import org.opentripplanner.routing.util.DiffTool;
import org.opentripplanner.utils.collection.CompositeComparator;
import org.opentripplanner.utils.text.Table;
import org.opentripplanner.utils.text.TableBuilder;
import org.opentripplanner.utils.time.DurationUtils;
import org.opentripplanner.utils.time.TimeUtils;

/**
 * This class is used to diff two set of paths. You may ask for the diff result or pass in a logger
 * to print the diff. Here is an example with two set with the to same paths in both, but where the
 * first path differ in cost:
 * <pre>
 * STATUS  | TX | DURATION |  COST | WALK |   START  |    END   | PATH
 * DROPPED |  1 |   21m26s | 22510 |  37s | 14:11:44 | 14:33:10 | Walk 1m16s ~ 21420 ~ BUS 51 14:13 14:29 ~ 2341 ~ Walk 4m10s [14:11:44 14:33:10 21m26s $22510]
 *   NEW   |  1 |   21m26s |  4195 |  37s | 14:11:44 | 14:33:10 | Walk 1m16s ~ 21420 ~ BUS 51 14:13 14:29 ~ 2341 ~ Walk 4m10s [14:11:44 14:33:10 21m26s $4195]
 *   BOTH  |  0 |    24m3s |  5085 |   0s | 14:09:07 | 14:33:10 | Walk 7m53s ~ 21251 ~ BUS 51 14:17 14:29 ~ 2341 ~ Walk 4m10s [14:09:07 14:33:10 24m3s $5085]
 * </pre>
 */
public class PathDiff<T extends RaptorTripSchedule> {

  /**
   * The status is not final; This allows to update the status when matching expected and actual
   * results.
   */
  public final RaptorPath<T> path;
  public final Integer walkDuration;
  public final List<String> routes = new ArrayList<>();
  public final List<Integer> stops = new ArrayList<>();

  private PathDiff(RaptorPath<T> path) {
    this.path = path;
    this.walkDuration =
      path
        .legStream()
        .filter(l -> l.isAccessLeg() || l.isTransferLeg() || l.isEgressLeg())
        .mapToInt(PathLeg::duration)
        .sum();
    this.routes.addAll(path.transitLegs().map(l -> l.trip().pattern().debugInfo()).toList());
    this.stops.addAll(path.listStops());
  }

  public static <T extends RaptorTripSchedule> void logDiff(
    String leftLabel,
    Collection<? extends RaptorPath<T>> left,
    String rightLabel,
    Collection<? extends RaptorPath<T>> right,
    boolean skipCost,
    boolean skipEquals,
    Consumer<String> logger
  ) {
    var result = diff(left, right, skipCost);

    // Walk* is access + transfer + egress time, independent of street mode - could be flex
    // or bycycle as well.
    TableBuilder tbl = Table
      .of()
      .withAlights(Center, Right, Right, Right, Right, Right, Right, Left)
      .withHeaders("STATUS", "TX", "Duration", "Cost", "Walk*", "Start", "End", "Path");

    for (DiffEntry<PathDiff<T>> e : result) {
      if (skipEquals && e.isEqual()) {
        continue;
      }
      PathDiff<? extends T> it = e.element();
      tbl.addRow(
        e.status("EQ", "DROPPED", "NEW"),
        it.path.numberOfTransfers(),
        DurationUtils.durationToStr(it.path.durationInSeconds()),
        it.path.c1(),
        DurationUtils.durationToStr(it.walkDuration),
        TimeUtils.timeToStrCompact(it.path.startTime()),
        TimeUtils.timeToStrCompact(it.path.endTime()),
        it.path.toString()
      );
    }
    logger.accept("Compare " + leftLabel + " with " + rightLabel + "\n" + tbl.toString());
  }

  public static <T extends RaptorTripSchedule> List<DiffEntry<PathDiff<T>>> diff(
    Collection<? extends RaptorPath<T>> left,
    Collection<? extends RaptorPath<T>> right,
    boolean skipCost
  ) {
    return DiffTool.diff(
      left.stream().map(PathDiff<T>::new).collect(Collectors.toList()),
      right.stream().map(PathDiff<T>::new).collect(Collectors.toList()),
      comparator(skipCost)
    );
  }

  public static <T extends RaptorTripSchedule> Comparator<PathDiff<T>> comparator(
    final boolean skipCost
  ) {
    return new CompositeComparator<>(
      Comparator.comparingInt(o -> o.path.endTime()),
      Comparator.comparingInt(o -> -o.path.startTime()),
      Comparator.comparingInt(o -> skipCost ? 0 : -o.path.c1()),
      (o1, o2) -> compareLists(o1.routes, o2.routes, String::compareTo),
      (o1, o2) -> compareLists(o1.stops, o2.stops, Integer::compareTo)
    );
  }

  private static <T> int compareLists(List<T> a, List<T> b, Comparator<T> comparator) {
    int size = Math.min(a.size(), b.size());
    for (int i = 0; i < size; i++) {
      int c = comparator.compare(a.get(i), b.get(i));
      if (c != 0) {
        return c;
      }
    }
    return a.size() - b.size();
  }
}
