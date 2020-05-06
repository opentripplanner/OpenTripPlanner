package org.opentripplanner.transit.raptor.speed_test;


import org.opentripplanner.transit.raptor.api.debug.DebugEvent;
import org.opentripplanner.transit.raptor.api.debug.DebugLogger;
import org.opentripplanner.transit.raptor.api.debug.DebugTopic;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.util.IntUtils;
import org.opentripplanner.transit.raptor.util.PathStringBuilder;
import org.opentripplanner.transit.raptor.util.TimeUtils;

import static org.opentripplanner.transit.raptor.util.TimeUtils.timeToStrCompact;

class SpeedTestDebugLogger<T extends RaptorTripSchedule> implements DebugLogger {
    private static int NOT_SET = Integer.MIN_VALUE;

    private final boolean enableDebugLogging;

    private int lastIterationTime = NOT_SET;
    private int lastRound = NOT_SET;
    private boolean pathHeader = true;

    SpeedTestDebugLogger(boolean enableDebugLogging) {
        this.enableDebugLogging = enableDebugLogging;
    }

    void stopArrivalLister(DebugEvent<ArrivalView<T>> e) {

        printIterationHeader(e.iterationStartTime());
        printRoundHeader(e.element().round());
        print(e.element(), e.action().toString(), e.reason());

        ArrivalView<?> byElement = e.rejectedDroppedByElement();
        if (e.action() == DebugEvent.Action.DROP && byElement != null) {
            print(byElement, "->by", "");
        }
    }

    void pathFilteringListener(DebugEvent<Path<T>> e) {
        if (pathHeader) {
            System.err.println();
            System.err.println(">>> PATH  | TR | FROM  | TO    | START    | END      | DURATION |   COST   | DETAILS");
            pathHeader = false;
        }

        Path<?> p = e.element();
        System.err.printf(
                "%9s | %2d | %5d | %5d | %8s | %8s | %8s | %8s | %s%n",
                center(e.action().toString(), 9),
                p.numberOfTransfers(),
                p.accessLeg().toStop(),
                p.egressLeg().fromStop(),
                TimeUtils.timeToStrLong(p.accessLeg().fromTime()),
                TimeUtils.timeToStrLong(p.egressLeg().toTime()),
                timeToStrCompact(p.travelDurationInSeconds()),
                p.cost(),
                details(e.action().toString(), e.reason(), e.toString())
        );
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
        String trip = a.arrivedByTransit() ? a.trip().debugInfo() : "";
        print(
                action,
                a.round(),
                a.legType(),
                a.stop(),
                a.arrivalTime(),
                a.cost(),
                trip,
                details(action, optReason, path(a))
        );
    }

    private static String details(String action, String optReason, String element) {
        return concat(optReason,  action + "ed element: " + element);
    }

    private static String path(ArrivalView<?> a) {
        return path(a, new PathStringBuilder()).toString()  + " (cost: " + a.cost() + ")";
    }

    private static PathStringBuilder path(ArrivalView<?> a, PathStringBuilder buf) {
        if (a.arrivedByAccessLeg()) {
            return buf.walk(legDuration(a)).sep().stop(a.stop());
        }
        // Recursively call this method to insert arrival in front of this arrival
        path(a.previous(), buf);

        buf.sep();

        if (a.arrivedByTransit()) {
            buf.transit(a.trip().pattern().modeInfo(), a.departureTime(), a.arrivalTime());
        } else {
            buf.walk(legDuration(a));
        }
        return buf.sep().stop(a.stop());
    }

    /**
     * The absolute time duration in seconds of a trip.
     */
    private static int legDuration(ArrivalView<?> a) {
        // Depending on the search direction this may or may not be negative, if we
        // search backwards in time then we arrive before we depart ;-) Hence
        // we need to use the absolute value.
        return Math.abs(a.arrivalTime() - a.departureTime());
    }

    private void printRoundHeader(int round) {
        if (round == lastRound) return;
        lastRound = round;

        System.err.println();
        System.err.printf(
                "%-9s | %-8s | %3s | %-5s | %-8s | %8s | %-24s | %s %n",
                "ARRIVAL",
                center("LEG", 8),
                "RND",
                "STOP",
                "ARRIVE",
                "COST",
                "TRIP",
                "DETAILS"
        );
    }

    private void print(String action, int round, String leg, int toStop, int toTime, int cost, String trip, String details) {
        System.err.printf(
                "%-9s | %-8s | %2d  | %5s | %8s | %8d | %-24s | %s %n",
                center(action, 9),
                center(leg, 8),
                round,
                IntUtils.intToString(toStop, NOT_SET),
                TimeUtils.timeToStrLong(toTime),
                cost,
                trip,
                details
        );
    }


    /** Inefficient simple implementation of this to avoid using apache commons. Used for debugging only. */
    private static String center(String text, int columnWidth) {
        if (text == null || columnWidth <= 0) {
            return text;
        }
        while (text.length() < columnWidth) {
            text += " ";
            if((text.length() < columnWidth)) {
                text = " " + text;
            }
        }
        return text;
    }

    private static String concat(String s, String t) {
        if(s == null || s.isEmpty()) {
            return t == null ? "" : t;
        }
        return s + ", " + (t == null ? "" : t);
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
}
