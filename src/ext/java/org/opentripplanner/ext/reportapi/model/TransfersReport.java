package org.opentripplanner.ext.reportapi.model;

import static org.opentripplanner.util.time.DurationUtils.durationToStr;

import java.util.List;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.WgsCoordinate;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.model.transfer.RouteStationTransferPoint;
import org.opentripplanner.model.transfer.RouteStopTransferPoint;
import org.opentripplanner.model.transfer.StationTransferPoint;
import org.opentripplanner.model.transfer.StopTransferPoint;
import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.model.transfer.TransferPoint;
import org.opentripplanner.model.transfer.TripTransferPoint;
import org.opentripplanner.routing.graph.GraphIndex;


/**
 * This class is used to export transfers for human verification to a CSV file. This is useful
 * when trying to debug the rather complicated NeTEx data format or to get the GTFS transfers in a
 * more human-readable form. It can also be used to test transfer functionality, since it is easy
 * to read and find special test-cases when needed.
 */
public class TransfersReport {
    private static final boolean BOARD = true;
    private static final boolean ALIGHT = false;
    private static final int NOT_SET = -1;


    private final List<ConstrainedTransfer> transfers;
    private final GraphIndex index;
    private final CsvReportBuilder buf = new CsvReportBuilder();

    private TransfersReport(
            List<ConstrainedTransfer> transfers,
            GraphIndex index
    ) {
        this.transfers = transfers;
        this.index = index;
    }

    public static String export(List<ConstrainedTransfer> transfers, GraphIndex index) {
        return new TransfersReport(transfers, index).export();
    }

    String export() {
        buf.addHeader(
                "Id", "Operator", "From", "FromId", "FromRoute", "FromTrip", "FromStop",
                "FromSpecificity", "To", "ToId", "ToRoute", "ToTrip", "ToStop", "ToSpecificity",
                "ArrivalTime", "DepartureTime", "TransferTime", "Walk", "Priority", "MaxWaitTime",
                "StaySeated", "Guaranteed"
        );

        transfers.forEach(t -> {
            var from = pointInfo(t.getFrom(), ALIGHT);
            var to = pointInfo(t.getTo(), BOARD);
            var dist = (from.coordinate == null || to.coordinate == null)
                    ? ""
                    : String.format(
                            "%.0fm",
                            SphericalDistanceLibrary.fastDistance(
                                    from.coordinate.asJtsCoordinate(),
                                    to.coordinate.asJtsCoordinate()
                            )
                    );
            var duration = (from.time == NOT_SET || to.time == NOT_SET)
                    ? "" : durationToStr(to.time - from.time);
            var c = t.getTransferConstraint();

            buf.addText(t.getId() == null ? "" : t.getId().getId());
            buf.addText((from.operator.isEmpty() ? to : from).operator);
            buf.addText(from.type);
            buf.addText(from.entityId);
            buf.addText(from.route);
            buf.addText(from.trip);
            buf.addText(from.location());
            buf.addNumber(from.specificity);
            buf.addText(to.type);
            buf.addText(to.entityId);
            buf.addText(to.route);
            buf.addText(to.trip);
            buf.addText(to.location());
            buf.addNumber(to.specificity);
            buf.addTime(from.time, NOT_SET);
            buf.addTime(to.time, NOT_SET);
            buf.addText(duration);
            buf.addText(dist);
            buf.addEnum(c.getPriority());
            buf.addDuration(c.getMaxWaitTime(), TransferConstraint.NOT_SET);
            buf.addOptText(c.isStaySeated(), "YES");
            buf.addOptText(c.isGuaranteed(), "YES");
            buf.newLine();
        });
        return buf.toString();
    }

    private TxPoint pointInfo(TransferPoint p, boolean boarding) {
        var r = new TxPoint();

        if(p instanceof TripTransferPoint) {
            var tp = (TripTransferPoint)p;
            var trip = tp.getTrip();
            var route = trip.getRoute();
            var ptn = index.getPatternForTrip().get(trip);
            r.operator = trip.getOperator().getId().getId();
            r.type = "Trip";
            r.entityId = trip.getId().getId();
            r.route = route.getName() + " " + route.getMode() + " " + route.getLongName();
            r.trip = trip.getTripHeadsign();
            var stop = ptn.getStop(tp.getStopPositionInPattern());
            addLocation(r, ptn, stop, trip, boarding);
        }
        else if(p instanceof RouteStopTransferPoint) {
            var rp = (RouteStopTransferPoint)p;
            var route = rp.getRoute();
            var ptn = index.getPatternsForRoute().get(route).stream().findFirst().orElse(null);
            r.operator = route.getOperator().getId().getId();
            r.type = "Route";
            r.entityId = route.getId().getId();
            r.route = route.getName() + " " + route.getMode() + " " + route.getLongName();
            addLocation(r, ptn, rp.getStop(), null, boarding);
        }
        else if(p instanceof RouteStationTransferPoint) {
            var rp = (RouteStationTransferPoint)p;
            var route = rp.getRoute();
            r.operator = route.getOperator().getId().getId();
            r.type = "Route";
            r.entityId = route.getId().getId();
            r.route = route.getName() + " " + route.getMode() + " " + route.getLongName();
            r.loc += rp.getStation().getName();
            r.coordinate = rp.getStation().getCoordinate();
        }
        else if(p instanceof StopTransferPoint) {
            var sp = (StopTransferPoint)p;
            StopLocation stop = sp.getStop();
            r.type = "Stop";
            r.entityId = stop.getId().getId();
            r.loc = stop.getName();
            r.coordinate = stop.getCoordinate();
        }
        else if(p instanceof StationTransferPoint) {
            var sp = (StationTransferPoint)p;
            Station station = sp.getStation();
            r.type = "Station";
            r.entityId = station.getId().getId();
            r.loc = station.getName();
            r.coordinate = station.getCoordinate();
        }

        r.specificity = p.getSpecificityRanking();
        r.coordinate = null;
        return r;
    }

    private static void addLocation(
            TxPoint r,
            TripPattern pattern,
            StopLocation stop,
            Trip trip,
            boolean boarding
    ) {
        if(pattern == null) {
            r.loc += stop.getName() + " [Pattern no found]";
            return;
        }
        int stopPosition = pattern.findStopPosition(stop);
        r.coordinate = stop.getCoordinate();

        if(stopPosition<0) {
            r.loc += "[Stop not found in pattern: " + stop.getName() + "]";
            return;
        }
        r.loc += stop.getName() + " [" + stopPosition + "]";

        if(trip != null) {
            var tt = pattern.getScheduledTimetable().getTripTimes(trip);
            r.time = boarding
                    ? tt.getScheduledDepartureTime(stopPosition)
                    : tt.getScheduledArrivalTime(stopPosition)
            ;
        }
    }

    static class TxPoint {
        private String operator = "";
        private String type = "";
        private String entityId = "";
        private String loc = "";
        private String trip = "";
        private String route = "";
        private Integer specificity = null;
        private WgsCoordinate coordinate = null;
        private int time = NOT_SET;

        String location() {
            return coordinate == null ? loc : loc + " " + coordinate;
        }
    }
}
