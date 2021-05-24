package org.opentripplanner.ext.reportapi.model;

import static org.opentripplanner.model.transfer.Transfer.MAX_WAIT_TIME_NOT_SET;
import static org.opentripplanner.util.time.DurationUtils.durationToStr;

import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.transfer.StopTransferPoint;
import org.opentripplanner.model.transfer.Transfer;
import org.opentripplanner.model.transfer.TransferPoint;
import org.opentripplanner.routing.graph.GraphIndex;


/**
 * This class is used to export transfers for human verification to a CSV file. This is useful
 * when trying to debug the rather complicated NeTEx data format or to get the GTFS transfers in a
 * more human readable form. It can also be used to test transfer functionality, since it is easy
 * to read and find special test-cases when needed.
 */
public class TransfersReport {
    private static final int NOT_SET = -1;


    private final List<Transfer> transfers;
    private final GraphIndex index;
    private final CsvReportBuilder buf = new CsvReportBuilder();

    private TransfersReport(
            List<Transfer> transfers,
            GraphIndex index
    ) {
        this.transfers = transfers;
        this.index = index;
    }

    public static String export(List<Transfer> transfers, GraphIndex index) {
        return new TransfersReport(transfers, index).export();
    }

    String export() {
        buf.addHeader(
                "Operator", "FromTripId", "FromTrip", "FromStop",
                "ToTripId", "ToTrip", "ToStop", "ArrivalTime", "DepartureTime", "TransferTime",
                "Walk", "Priority", "MaxWaitTime", "StaySeated", "Guaranteed"
        );

        transfers.forEach(t -> {
            var from = pointInfo(t.getFrom(), true);
            var to = pointInfo(t.getTo(), false);
            var dist = (from.c == null || to.c == null)
                    ? ""
                    : String.format(
                            "%.0fm",
                            SphericalDistanceLibrary.fastDistance(from.c, to.c)
                    );
            var duration = (from.time == NOT_SET || to.time == NOT_SET)
                    ? "" : durationToStr(to.time - from.time);

            buf.addText(t.getFrom().getTrip().getOperator().getId().getId());
            buf.addText(from.tripId);
            buf.addText(from.trip);
            buf.addText(from.loc);
            buf.addText(to.tripId);
            buf.addText(to.trip);
            buf.addText(to.loc);
            buf.addTime(from.time, NOT_SET);
            buf.addTime(to.time, NOT_SET);
            buf.addText(duration);
            buf.addText(dist);
            buf.addEnum(t.getPriority());
            buf.addDuration(t.getMaxWaitTime(), MAX_WAIT_TIME_NOT_SET);
            buf.addOptText(t.isStaySeated(), "YES");
            buf.addOptText(t.isGuaranteed(), "YES");
            buf.newLine();
        });
        return buf.toString();
    }

    private TxPoint pointInfo(
            TransferPoint p,
            boolean arrival
    ) {
        var r = new TxPoint();
        if (p instanceof StopTransferPoint) {
            r.loc = p.getStop().getName();
            return r;
        }
        var ptn = index.getPatternForTrip().get(p.getTrip());
        var trip = p.getTrip();
        var route = trip.getRoute();

        r.tripId = trip.getId().getId();
        r.trip = route.getName() + " " + route.getMode() + " " + route.getLongName()
                + " " + trip.getTripHeadsign();
        r.c = null;




        if (ptn.getStops().size() > p.getStopPosition()) {
            int pos = p.getStopPosition();
            Stop stop = ptn.getStops().get(pos);
            var tt = ptn.scheduledTimetable.getTripTimes(trip);
            r.loc += stop.getName() + " " + stop.getCoordinate();
            r.time = arrival ? tt.getScheduledArrivalTime(pos) : tt.getScheduledDepartureTime(pos);
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
