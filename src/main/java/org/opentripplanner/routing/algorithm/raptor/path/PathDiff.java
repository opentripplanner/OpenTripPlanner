package org.opentripplanner.routing.algorithm.raptor.path;

import static org.opentripplanner.util.TableFormatter.Align.Center;
import static org.opentripplanner.util.TableFormatter.Align.Left;
import static org.opentripplanner.util.TableFormatter.Align.Right;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.opentripplanner.routing.util.DiffTool;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.util.CompositeComparator;
import org.opentripplanner.util.TableFormatter;
import org.opentripplanner.util.time.DurationUtils;
import org.opentripplanner.util.time.TimeUtils;


/**
 * This class is used to diff two set of paths. You may ask for the diff result
 * or pass in a logger to print the diff. Here is an example with two set with the to same paths
 * in both, but where the first path differ in cost:
 * <pre>
 * STATUS | TX | DURATION |  COST | WALK |   START  |    END   | PATH
 *  RIGHT |  1 |   21m26s | 22510 |  37s | 14:11:44 | 14:33:10 | Walk 1m16s ~ 21420 ~ BUS 51 14:13 14:29 ~ 2341 ~ Walk 4m10s [14:11:44 14:33:10 21m26s $22510]
 *  LEFT  |  1 |   21m26s |  4195 |  37s | 14:11:44 | 14:33:10 | Walk 1m16s ~ 21420 ~ BUS 51 14:13 14:29 ~ 2341 ~ Walk 4m10s [14:11:44 14:33:10 21m26s $4195]
 *   EQ   |  0 |    24m3s |  5085 |   0s | 14:09:07 | 14:33:10 | Walk 7m53s ~ 21251 ~ BUS 51 14:17 14:29 ~ 2341 ~ Walk 4m10s [14:09:07 14:33:10 24m3s $5085]
 * </pre>
 *
 * @param <T>
 */
public class PathDiff<T extends RaptorTripSchedule> {

  /**
   * The status is not final; This allows to update the status when matching expected and actual
   * results.
   */
  public final Path<T> path;
  public final Integer walkDuration;
  public final List<String> routes = new ArrayList<>();
  public final List<Integer> stops = new ArrayList<>();

  private PathDiff(Path<T> path) {
    this.path = path;
    this.walkDuration = path
        .legStream()
        .filter(PathLeg::isTransferLeg)
        .mapToInt(l -> l.asTransferLeg().duration())
        .sum();
    this.routes.addAll(
        path.legStream()
            .filter(PathLeg::isTransitLeg)
            .map(l -> l.asTransitLeg().trip().pattern().debugInfo())
            .collect(Collectors.toList())
    );
    this.stops.addAll(path.listStops());
  }

  public static <T extends RaptorTripSchedule> void logDiff(
      String leftLabel,
      Collection<? extends Path<T>> left,
      String rightLabel,
      Collection<? extends Path<T>> right,
      boolean skipCost,
      boolean skipEquals,
      Consumer<String> logger
  ) {
    var result = diff(left, right, skipCost);

    TableFormatter tbl = new TableFormatter(
        List.of(Center, Right, Right, Right, Right, Right, Right, Left),
        List.of("STATUS", "TX", "Duration", "Cost", "Walk", "Start", "End", "Path")
    );

    for (DiffTool.Entry<PathDiff<T>> e : result) {
      if(skipEquals && e.isEqual()) { continue; }
      PathDiff<? extends T> it = e.element();
      tbl.addRow(
        e.status("OK", "DROPPED", "NEW"),
        it.path.numberOfTransfers(),
        DurationUtils.durationToStr(it.path.travelDurationInSeconds()),
        it.path.generalizedCost(),
        DurationUtils.durationToStr(it.walkDuration),
        TimeUtils.timeToStrCompact(it.path.startTime()),
        TimeUtils.timeToStrCompact(it.path.endTime()),
        it.path.toString()
      );
    }
    logger.accept("Compare " + leftLabel + " with " + rightLabel + "\n" + tbl.toString());
  }

  public static <T extends RaptorTripSchedule> List<DiffTool.Entry<PathDiff<T>>> diff(
      Collection<? extends Path<T>> left, Collection<? extends Path<T>> right, boolean skipCost
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
        Comparator.comparingInt(o -> skipCost ? 0 : -o.path.generalizedCost()),
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
