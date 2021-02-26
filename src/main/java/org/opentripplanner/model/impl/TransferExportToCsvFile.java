package org.opentripplanner.model.impl;

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
 * This class can be used to export transfers for human verification. This is useful when trying
 * to debug the rather complicated NeTEx data format.
 */
class TransferExportToCsvFile {

    static final Logger LOG = LoggerFactory.getLogger(TransferExportToCsvFile.class);

    static void exportTransfers(File file, List<Transfer> transfers, TripStopTimes stopTimesByTrip) {
        try(PrintWriter p = new PrintWriter(file)) {
            LOG.info("Transfers total: " + transfers.size());
            LOG.info("Transfers dumped: " + file.getAbsolutePath());

            p.println("Op;FromTrip;FromStop;ToTrip;ToStop;ArrivalTime;DepartureTime;TransferTime;Walk;Priority;MaxWaitTime;StaySeated;Guaranteed;");

            transfers.forEach(t -> {
                var from = pointInfo(stopTimesByTrip, t.getFrom(), true);
                var to = pointInfo(stopTimesByTrip, t.getTo(), false);
                var dist = (from.c == null || to.c == null)
                        ? ""
                        : String.format("%.0fm",
                                SphericalDistanceLibrary.fastDistance(from.c, to.c)
                        );
                var time = (from.time == -1 || to.time == -1)
                        ? "" : DurationUtils.durationToStr(to.time - from.time);

                p.println(
                        t.getFrom().getTrip().getOperator().getId().getId().substring(0, 3) + ";"
                                + from.trip + ";" + from.loc + ";"
                                + to.trip + ";" + to.loc + ";"
                                + TimeUtils.timeToStrCompact(from.time, -1) + ";"
                                + TimeUtils.timeToStrCompact(to.time, -1) + ";"
                                + time + ";" + dist + ";"
                                + t.getPriority() + ";"
                                + t.getMaxWaitTime() + ";"
                                + (t.isStaySeated() ? "TRUE;" : ";")
                                + (t.isGuaranteed() ? "TRUE;" : ";")
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

        r.trip = route.getName() +  " " + route.getMode() + " " + route.getLongName();
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
        private String trip = "";
        private Coordinate c = null;
        private int time = -1;
    }
}
