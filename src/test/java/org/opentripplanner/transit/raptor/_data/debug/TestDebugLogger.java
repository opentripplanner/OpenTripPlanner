package org.opentripplanner.transit.raptor._data.debug;


import static org.opentripplanner.util.TableFormatter.Align.Center;
import static org.opentripplanner.util.TableFormatter.Align.Left;
import static org.opentripplanner.util.TableFormatter.Align.Right;
import static org.opentripplanner.util.time.TimeUtils.timeToStrCompact;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import org.opentripplanner.transit.raptor.api.debug.DebugEvent;
import org.opentripplanner.transit.raptor.api.debug.DebugLogger;
import org.opentripplanner.transit.raptor.api.debug.DebugTopic;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.request.DebugRequestBuilder;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.rangeraptor.multicriteria.PatternRide;
import org.opentripplanner.transit.raptor.rangeraptor.transit.BoarAndAlightTime;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TripTimesSearch;
import org.opentripplanner.transit.raptor.speed_test.SpeedTest;
import org.opentripplanner.transit.raptor.util.IntUtils;
import org.opentripplanner.transit.raptor.util.PathStringBuilder;
import org.opentripplanner.util.TableFormatter;
import org.opentripplanner.util.time.DurationUtils;
import org.opentripplanner.util.time.TimeUtils;


/**
 * A debug logger witch can be plugged into Raptor to do debug logging to standard error. This
 * is used by the {@link SpeedTest} and in module tests.
 * <p>
 * See the Raptor design doc for a general description of the logging functionality.
 */
public class TestDebugLogger implements DebugLogger {
    private static final int NOT_SET = Integer.MIN_VALUE;

    private final boolean enableDebugLogging;
    private final NumberFormat numFormat = NumberFormat.getInstance(Locale.FRANCE);

    private int lastIterationTime = NOT_SET;
    private int lastRound = NOT_SET;
    private boolean pathHeader = true;

    private final TableFormatter arrivalTableFormatter = new TableFormatter(
        List.of(Center, Center, Right, Right, Right, Right, Left, Left),
        List.of("ARRIVAL", "LEG", "RND", "STOP", "ARRIVE", "COST", "TRIP", "DETAILS"),
        9, 7, 3, 5, 8, 9, 24, 0
    );

    private final TableFormatter pathTableFormatter = new TableFormatter(
        List.of(Center, Center, Right, Right, Right, Right, Right, Right, Left),
        List.of(">>> PATH", "TR", "FROM", "TO", "START", "END", "DURATION", "COST", "DETAILS"),
        9, 2, 5, 5, 8, 8, 8, 6, 0
    );

    public TestDebugLogger(boolean enableDebugLogging) {
        this.enableDebugLogging = enableDebugLogging;
    }

    /**
     * This should be passed into the {@link DebugRequestBuilder#stopArrivalListener(Consumer)}
     * using a lambda to enable debugging stop arrivals.
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
    public void patternRideLister(DebugEvent<PatternRide<?>> e) {
        printIterationHeader(e.iterationStartTime());
        printRoundHeader(e.element().prevArrival.round() + 1);
        print(e.element(), e.action().toString());

        PatternRide<?> byElement = e.rejectedDroppedByElement();
        if (e.action() == DebugEvent.Action.DROP && byElement != null) {
            print(byElement, "->by");
        }
    }

    /**
     * This should be passed into the {@link DebugRequestBuilder#pathFilteringListener(Consumer)}
     * using a lambda to enable debugging paths put in the final result pareto-set.
     */
    public void pathFilteringListener(DebugEvent<Path<?>> e) {
        if (pathHeader) {
            System.err.println();
            System.err.println(pathTableFormatter.printHeader());
            pathHeader = false;
        }

        Path<?> p = e.element();
      System.err.println(
            pathTableFormatter.printRow(
                e.action().toString(),
                p.numberOfTransfers(),
                p.accessLeg().toStop(),
                p.egressLeg().fromStop(),
                TimeUtils.timeToStrLong(p.accessLeg().fromTime()),
                TimeUtils.timeToStrLong(p.egressLeg().toTime()),
                DurationUtils.durationToStr(p.travelDurationInSeconds()),
                numFormat.format(p.generalizedCost()),
                details(e.action().toString(), e.reason(), e.element().toString())
            )
        );
    }

    @Override
    public boolean isEnabled(DebugTopic topic) {
        return enableDebugLogging;
    }

    @Override
    public void debug(DebugTopic topic, String message) {
        if(enableDebugLogging) {
            // We log to info - since debugging is controlled by the application
            if(message.contains("\n")) {
                System.err.printf("%s\n%s", topic, message);
            }
            else {
                System.err.printf("%-16s | %s%n", topic, message);
            }
        }
    }


    /* private methods */

    private void printIterationHeader(int iterationTime) {
        if (iterationTime == lastIterationTime) return;
        lastIterationTime = iterationTime;
        lastRound = NOT_SET;
        pathHeader = true;
        System.err.println("\n**  RUN RAPTOR FOR MINUTE: " + timeToStrCompact(iterationTime) + "  **");
    }

    private void print(ArrivalView<?> a, String action, String optReason) {
        String pattern = a.arrivedByTransit() ? a.transitPath().trip().pattern().debugInfo() : "";
        System.err.println(
            arrivalTableFormatter.printRow(
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

    private void print(PatternRide<?> p, String action) {
        System.err.println(
            arrivalTableFormatter.printRow(
                action,
                "OnRide",
                p.prevArrival.round() + 1,
                p.boardStopIndex,
                TimeUtils.timeToStrLong(p.boardTime),
                numFormat.format(p.relativeCost),
                p.trip.pattern().debugInfo(),
                p.toString()
            )
        );
    }

    private static String details(String action, String optReason, String element) {
        return concat(optReason,  action + ", element: " + element);
    }

    private static String path(ArrivalView<?> a) {
        return path(a, new PathStringBuilder()).toString()  + " (cost: " + a.cost() + ")";
    }

    private static PathStringBuilder path(ArrivalView<?> a, PathStringBuilder buf) {
        if (a.arrivedByAccess()) {
            return buf.accessEgress(a.accessPath().access()).sep().stop(a.stop());
        }
        // Recursively call this method to insert arrival in front of this arrival
        path(a.previous(), buf);

        buf.sep();

        if (a.arrivedByTransit()) {
            // forward search
            String tripInfo = a.transitPath().trip().pattern().debugInfo();
            if(a.arrivalTime() > a.previous().arrivalTime()) {
                BoarAndAlightTime t = TripTimesSearch.findTripForwardSearch(a);
                buf.transit(tripInfo, t.boardTime(), t.alightTime());
            }
            // reverse search
            else {
                BoarAndAlightTime t = TripTimesSearch.findTripReverseSearch(a);
                buf.transit(tripInfo, t.alightTime(), t.boardTime());
            }
        } else if(a.arrivedByTransfer()) {
            buf.walk(legDuration(a));
        } else {
            buf.accessEgress(a.egressPath().egress());
        }

        return buf.sep().stop(a.stop());
    }

    /**
     * The absolute time duration in seconds of a trip.
     */
    private static int legDuration(ArrivalView<?> a) {
        if(a.arrivedByAccess()) {
            return a.accessPath().access().durationInSeconds();
        }
        if(a.arrivedByTransfer()) {
            return a.transferPath().durationInSeconds();
        }
        if(a.arrivedAtDestination()) {
            return a.egressPath().egress().durationInSeconds();
        }
        throw new IllegalStateException("Unsuported type: " + a.getClass());
    }

    private void printRoundHeader(int round) {
        if (round == lastRound) return;
        lastRound = round;

        System.err.println();
        System.err.println(arrivalTableFormatter.printHeader());
    }

    private static String concat(String s, String t) {
        if(s == null || s.isEmpty()) {
            return t == null ? "" : t;
        }
        return s + ", " + (t == null ? "" : t);
    }

    private String legType(ArrivalView<?> a) {
        if (a.arrivedByAccess()) { return "Access"; }
        if (a.arrivedByTransit()) { return "Transit"; }
        // We use Walk instead of Transfer so it is easier to distinguish from Transit
        if (a.arrivedByTransfer()) { return "Walk"; }
        if (a.arrivedAtDestination()) { return "Egress"; }
        throw new IllegalStateException("Unknown mode for: " + this);
    }
}
