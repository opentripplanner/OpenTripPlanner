package org.opentripplanner.model.impl;

import static org.opentripplanner.model.transfer.Transfer.MAX_WAIT_TIME_NOT_SET;
import static org.opentripplanner.util.time.DurationUtils.durationToStr;
import static org.opentripplanner.util.time.TimeUtils.timeToStrCompact;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.TripStopTimes;
import org.opentripplanner.model.transfer.StopTransferPoint;
import org.opentripplanner.model.transfer.Transfer;
import org.opentripplanner.model.transfer.TransferPoint;
import org.opentripplanner.util.time.DurationUtils;
import org.opentripplanner.util.time.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class is used to export transfers for human verification. This is useful when trying
 * to debug the rather complicated NeTEx data format or to get the GTFS transfers in a more human
 * readable form. It can also be used to test transfer functionality, since it is easy to read and
 * find special test-cases when needed.
 */
class TransferExport {

    private static final Logger LOG = LoggerFactory.getLogger("TRANSFERS_EXPORT");
    private static final File FILE = new File("transfers-debug.csv");
    private static final String SEP = ";";
    private static final int NOT_SET = -1;

    static void exportTransfers(List<Transfer> transfers, TripStopTimes stopTimesByTrip) {
    if (!TransferExport.LOG.isDebugEnabled()) { return; }

    try(PrintWriter p = new PrintWriter(FILE)) {
            LOG.info("Transfers total: " + transfers.size());
            LOG.info("Transfers dumped: " + FILE.getAbsolutePath());

            p.println("Op;FromTripId;FromTrip;FromStop;ToTripId;ToTrip;ToStop;ArrivalTime;DepartureTime;TransferTime;Walk;Priority;MaxWaitTime;StaySeated;Guaranteed;");

            transfers.forEach(t -> {
                var from = pointInfo(stopTimesByTrip, t.getFrom(), true);
                var to = pointInfo(stopTimesByTrip, t.getTo(), false);
                var dist = (from.c == null || to.c == null)
                        ? ""
                        : String.format("%.0fm",
                                SphericalDistanceLibrary.fastDistance(from.c, to.c)
                        );
                var time = (from.time == NOT_SET || to.time == NOT_SET)
                        ? "" : durationToStr(to.time - from.time);

                p.println(
                        t.getFrom().getTrip().getOperator().getId().getId().substring(0, 3) + SEP
                                + from.tripId + SEP + from.trip + SEP + from.loc + SEP
                                + to.tripId + SEP + to.trip + SEP + to.loc + SEP
                                + timeToStrCompact(from.time, NOT_SET) + SEP
                                + timeToStrCompact(to.time, NOT_SET) + SEP
                                + time + SEP + dist + SEP
                                + t.getPriority() + SEP
                                + durationToStr(t.getMaxWaitTime(), MAX_WAIT_TIME_NOT_SET) + SEP
                                + (t.isStaySeated() ? "YES;" : SEP)
                                + (t.isGuaranteed() ? "YES;" : SEP)
                );
            });
            p.flush();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static TxPoint pointInfo(TripStopTimes stopTimesByTrip, TransferPoint p, boolean arrival) {
        var r = new TxPoint();
        if(p instanceof StopTransferPoint) {
            r.loc = p.getStop().getName();
            return r;
        }
        var s = stopTimesByTrip.get(p.getTrip());
        var trip = p.getTrip();
        var route = trip.getRoute();

        r.tripId = trip.getId().getId();
        r.trip = route.getName() +  " " + route.getMode() + " " + route.getLongName()
                + " " + trip.getTripHeadsign();
        r.c = null;
        if(s.size() > p.getStopPosition()) {
            StopTime stopTime = s.get(p.getStopPosition());
            StopLocation stop = stopTime.getStop();
            r.loc += stop.getName() + " " + stop.getCoordinate();
            r.time = arrival ? stopTime.getArrivalTime() : stopTime.getDepartureTime();
            r.c = stop.getCoordinate().asJtsCoordinate();
        }
        else {
            r.loc += "[Stop index not found: " + p.getStopPosition() + "]";
        }
        r.loc += " " + p.getSpecificityRanking();
        return r;
    }

    static class TxPoint {
        private String loc = "";
        private String tripId = "";
        private String trip = "";
        private Coordinate c = null;
        private int time = NOT_SET;
    }
}
