package org.opentripplanner.ext.reportapi.model;

import static org.opentripplanner.utils.time.DurationUtils.durationToStr;

import java.util.List;
import java.util.Optional;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.model.transfer.RouteStationTransferPoint;
import org.opentripplanner.model.transfer.RouteStopTransferPoint;
import org.opentripplanner.model.transfer.StationTransferPoint;
import org.opentripplanner.model.transfer.StopTransferPoint;
import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.model.transfer.TransferPoint;
import org.opentripplanner.model.transfer.TripTransferPoint;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.TransitService;

/**
 * This class is used to export transfers for human verification to a CSV file. This is useful when
 * trying to debug the rather complicated NeTEx data format or to get the GTFS transfers in a more
 * human-readable form. It can also be used to test transfer functionality, since it is easy to read
 * and find special test-cases when needed.
 */
public class TransfersReport {

  private static final boolean BOARD = true;
  private static final boolean ALIGHT = false;
  private static final int NOT_SET = -1;

  private final List<ConstrainedTransfer> transfers;
  private final TransitService transitService;
  private final CsvReportBuilder buf = new CsvReportBuilder();

  private TransfersReport(List<ConstrainedTransfer> transfers, TransitService transitService) {
    this.transfers = transfers;
    this.transitService = transitService;
  }

  public static String export(List<ConstrainedTransfer> transfers, TransitService transitService) {
    return new TransfersReport(transfers, transitService).export();
  }

  String export() {
    buf.addHeader(
      "Id",
      "Operator",
      "From",
      "FromId",
      "FromRoute",
      "FromTrip",
      "FromStop",
      "FromSpecificity",
      "To",
      "ToId",
      "ToRoute",
      "ToTrip",
      "ToStop",
      "ToSpecificity",
      "ArrivalTime",
      "DepartureTime",
      "TransferTime",
      "Walk",
      "Priority",
      "MaxWaitTime",
      "StaySeated",
      "Guaranteed"
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
        ? ""
        : durationToStr(to.time - from.time);
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

  private static void addLocation(
    TxPoint r,
    TripPattern pattern,
    StopLocation stop,
    Trip trip,
    boolean boarding
  ) {
    if (pattern == null) {
      r.loc += stop.getName() + " [Pattern no found]";
      return;
    }
    int stopPosition = pattern.findStopPosition(stop);
    r.coordinate = stop.getCoordinate();

    if (stopPosition < 0) {
      r.loc += "[Stop not found in pattern: " + stop.getName() + "]";
      return;
    }
    r.loc += stop.getName() + " [" + stopPosition + "]";

    if (trip != null) {
      var tt = pattern.getScheduledTimetable().getTripTimes(trip);
      r.time = boarding
        ? tt.getScheduledDepartureTime(stopPosition)
        : tt.getScheduledArrivalTime(stopPosition);
    }
  }

  private TxPoint pointInfo(TransferPoint p, boolean boarding) {
    var r = new TxPoint();

    if (p instanceof TripTransferPoint tp) {
      var trip = tp.getTrip();
      var route = trip.getRoute();
      var ptn = transitService.findPattern(trip);
      r.operator = getName(trip.getOperator());
      r.type = "Trip";
      r.entityId = trip.getId().getId();
      r.route = route.getName() + " " + route.getMode() + " " + route.getLongName();
      r.trip = trip.getHeadsign() != null ? trip.getHeadsign().toString() : null;
      var stop = ptn.getStop(tp.getStopPositionInPattern());
      addLocation(r, ptn, stop, trip, boarding);
    } else if (p instanceof RouteStopTransferPoint rp) {
      var route = rp.getRoute();
      var ptn = transitService.findPatterns(route).stream().findFirst().orElse(null);
      r.operator = getName(route.getOperator());
      r.type = "Route";
      r.entityId = route.getId().getId();
      r.route = route.getName() + " " + route.getMode() + " " + route.getLongName();
      addLocation(r, ptn, rp.getStop(), null, boarding);
    } else if (p instanceof RouteStationTransferPoint rp) {
      var route = rp.getRoute();
      r.operator = getName(route.getOperator());
      r.type = "Route";
      r.entityId = route.getId().getId();
      r.route = route.getName() + " " + route.getMode() + " " + route.getLongName();
      r.loc += rp.getStation().getName();
      r.coordinate = rp.getStation().getCoordinate();
    } else if (p instanceof StopTransferPoint sp) {
      StopLocation stop = sp.getStop();
      r.type = "Stop";
      r.entityId = stop.getId().getId();
      r.loc = Optional.ofNullable(stop.getName()).map(I18NString::toString).orElse("");
      r.coordinate = stop.getCoordinate();
    } else if (p instanceof StationTransferPoint sp) {
      Station station = sp.getStation();
      r.type = "Station";
      r.entityId = station.getId().getId();
      r.loc = station.getName().toString();
      r.coordinate = station.getCoordinate();
    }

    r.specificity = p.getSpecificityRanking();
    r.coordinate = null;
    return r;
  }

  private static String getName(Operator operator) {
    return Optional.ofNullable(operator).map(o -> o.getId().getId()).orElse("");
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
