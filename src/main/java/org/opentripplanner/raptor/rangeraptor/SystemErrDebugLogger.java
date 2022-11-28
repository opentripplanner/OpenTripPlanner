package org.opentripplanner.raptor.rangeraptor;

import static org.opentripplanner.framework.text.Table.Align.Center;
import static org.opentripplanner.framework.text.Table.Align.Left;
import static org.opentripplanner.framework.text.Table.Align.Right;
import static org.opentripplanner.framework.time.TimeUtils.timeToStrCompact;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.opentripplanner.framework.lang.IntUtils;
import org.opentripplanner.framework.lang.OtpNumberFormat;
import org.opentripplanner.framework.lang.StringUtils;
import org.opentripplanner.framework.text.Table;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.raptor.api.debug.DebugEvent;
import org.opentripplanner.raptor.api.debug.DebugLogger;
import org.opentripplanner.raptor.api.debug.DebugTopic;
import org.opentripplanner.raptor.api.path.Path;
import org.opentripplanner.raptor.api.path.PathStringBuilder;
import org.opentripplanner.raptor.api.request.DebugRequestBuilder;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.api.view.PatternRideView;
import org.opentripplanner.raptor.rangeraptor.transit.TripTimesSearch;

/**
 * A debug logger witch can be plugged into Raptor to do debug logging to standard error. This is
 * used by the REST API, SpeedTest and in module tests.
 * <p>
 * See the Raptor design doc for a general description of the logging functionality.
 */
public class SystemErrDebugLogger implements DebugLogger {

  private static final int NOT_SET = Integer.MIN_VALUE;

  private final boolean enableDebugLogging;
  private final NumberFormat numFormat = NumberFormat.getInstance(Locale.FRANCE);
  private final Table arrivalTable = Table
    .of()
    .withAlights(Center, Center, Right, Right, Right, Right, Left, Left)
    .withHeaders("ARRIVAL", "LEG", "RND", "STOP", "ARRIVE", "COST", "TRIP", "DETAILS")
    .withMinWidths(9, 7, 3, 5, 8, 9, 24, 0)
    .build();
  private final Table pathTable = Table
    .of()
    .withAlights(Center, Center, Right, Right, Right, Right, Right, Right, Left)
    .withHeaders(">>> PATH", "TR", "FROM", "TO", "START", "END", "DURATION", "COST", "DETAILS")
    .withMinWidths(9, 2, 5, 5, 8, 8, 8, 6, 0)
    .build();
  private boolean forwardSearch = true;
  private int lastIterationTime = NOT_SET;
  private int lastRound = NOT_SET;
  private boolean printPathHeader = true;

  public SystemErrDebugLogger(boolean enableDebugLogging) {
    this.enableDebugLogging = enableDebugLogging;
  }

  /**
   * This should be passed into the {@link DebugRequestBuilder#stopArrivalListener(Consumer)} using
   * a lambda to enable debugging stop arrivals.
   */
  public void stopArrivalLister(DebugEvent<ArrivalView<?>> e) {
    printIterationHeader(e.iterationStartTime());
    printRoundHeader(e.element().round());
    print(e.element(), e.action().toString(), e.reason());

    ArrivalView<?> byElement = e.rejectedDroppedByElement();
    if (e.action() == DebugEvent.Action.DROP && byElement != null) {
      print(byElement, "->by", "");
    }
  }

  /**
   * This should be passed into the {@link DebugRequestBuilder#patternRideDebugListener(Consumer)}
   * using a lambda to enable debugging pattern ride events.
   */
  public void patternRideLister(DebugEvent<PatternRideView<?>> e) {
    printIterationHeader(e.iterationStartTime());
    printRoundHeader(e.element().prevArrival().round() + 1);
    print(e.element(), e.action().toString());

    PatternRideView<?> byElement = e.rejectedDroppedByElement();
    if (e.action() == DebugEvent.Action.DROP && byElement != null) {
      print(byElement, "->by");
    }
  }

  /**
   * This should be passed into the {@link DebugRequestBuilder#pathFilteringListener(Consumer)}
   * using a lambda to enable debugging paths put in the final result pareto-set.
   */
  public void pathFilteringListener(DebugEvent<Path<?>> e) {
    if (printPathHeader) {
      System.err.println();
      System.err.println(pathTable.headerRow());
      printPathHeader = false;
    }

    Path<?> p = e.element();
    System.err.println(
      pathTable.rowAsText(
        e.action().toString(),
        p.numberOfTransfers(),
        p.accessLeg().toStop(),
        p.egressLeg().fromStop(),
        TimeUtils.timeToStrLong(p.accessLeg().fromTime()),
        TimeUtils.timeToStrLong(p.egressLeg().toTime()),
        DurationUtils.durationToStr(p.durationInSeconds()),
        OtpNumberFormat.formatCostCenti(p.generalizedCost()),
        details(e.action().toString(), e.reason(), e.element().toString())
      )
    );
  }

  @Override
  public boolean isEnabled() {
    return enableDebugLogging;
  }

  @Override
  public void setSearchDirection(boolean forward) {
    this.forwardSearch = forward;
  }

  @Override
  public void debug(DebugTopic topic, String message) {
    if (enableDebugLogging) {
      // We log to info - since debugging is controlled by the application
      if (message.contains("\n")) {
        System.err.printf("%s\n%s", topic, message);
      } else {
        System.err.printf("%-16s | %s%n", topic, message);
      }
    }
  }

  /* private methods */

  private static String details(String action, String optReason, @Nullable String element) {
    StringBuilder buf = new StringBuilder();
    buf.append(action).append(": ").append(element);
    if (isNotBlank(optReason)) {
      buf.append("  # ").append(optReason);
    }
    return buf.toString();
  }

  /**
   * The absolute time duration in seconds of a trip.
   */
  private static int legDuration(ArrivalView<?> a) {
    if (a.arrivedByAccess()) {
      return a.accessPath().access().durationInSeconds();
    }
    if (a.arrivedByTransfer()) {
      return a.transferPath().durationInSeconds();
    }
    if (a.arrivedAtDestination()) {
      return a.egressPath().egress().durationInSeconds();
    }
    throw new IllegalStateException("Unsupported type: " + a.getClass());
  }

  private static boolean isNotBlank(String text) {
    return StringUtils.hasValue(text);
  }

  private void printIterationHeader(int iterationTime) {
    if (iterationTime == lastIterationTime) {
      return;
    }
    lastIterationTime = iterationTime;
    lastRound = NOT_SET;
    printPathHeader = true;
    System.err.println("\n**  RUN RAPTOR FOR MINUTE: " + timeToStrCompact(iterationTime) + "  **");
  }

  private void print(ArrivalView<?> a, String action, String optReason) {
    printPathHeader = true;
    String pattern = a.arrivedByTransit() ? a.transitPath().trip().pattern().debugInfo() : "";
    System.err.println(
      arrivalTable.rowAsText(
        action,
        legType(a),
        a.round(),
        IntUtils.intToString(a.stop(), NOT_SET),
        TimeUtils.timeToStrLong(a.arrivalTime()),
        numFormat.format(a.cost()),
        pattern,
        details(action, optReason, path(a))
      )
    );
  }

  private void print(PatternRideView<?> p, String action) {
    System.err.println(
      arrivalTable.rowAsText(
        action,
        "OnRide",
        p.prevArrival().round() + 1,
        p.boardStopIndex(),
        TimeUtils.timeToStrLong(p.boardTime()),
        numFormat.format(p.relativeCost()),
        p.trip().pattern().debugInfo(),
        p.toString()
      )
    );
  }

  private String path(ArrivalView<?> a) {
    return path(a, new PathStringBuilder(null))
      .space()
      .space()
      .append("[ ")
      .generalizedCostSentiSec(a.cost())
      .append(" ]")
      .toString();
  }

  private PathStringBuilder path(ArrivalView<?> a, PathStringBuilder buf) {
    if (a.arrivedByAccess()) {
      return buf.accessEgress(a.accessPath().access()).sep().stop(a.stop());
    }
    // Recursively call this method to insert arrival in front of this arrival
    path(a.previous(), buf);

    buf.sep();

    if (a.arrivedByTransit()) {
      String tripInfo = a.transitPath().trip().pattern().debugInfo();

      if (forwardSearch) {
        var t = TripTimesSearch.findTripForwardSearchApproximateTime(a);
        buf.transit(tripInfo, t.boardTime(), t.alightTime());
      }
      // reverse search
      else {
        var t = TripTimesSearch.findTripReverseSearchApproximateTime(a);
        buf.transit(tripInfo, t.alightTime(), t.boardTime());
      }
    } else if (a.arrivedByTransfer()) {
      buf.walk(legDuration(a));
    } else {
      buf.accessEgress(a.egressPath().egress());
    }

    return buf.sep().stop(a.stop());
  }

  private void printRoundHeader(int round) {
    if (round == lastRound) {
      return;
    }
    lastRound = round;

    System.err.println();
    System.err.println(arrivalTable.headerRow());
  }

  private String legType(ArrivalView<?> a) {
    if (a.arrivedByAccess()) {
      return "Access";
    }
    if (a.arrivedByTransit()) {
      return "Transit";
    }
    // We use Walk instead of Transfer so it is easier to distinguish from Transit
    if (a.arrivedByTransfer()) {
      return "Walk";
    }
    if (a.arrivedAtDestination()) {
      return "Egress";
    }
    throw new IllegalStateException("Unknown mode for: " + this);
  }
}
