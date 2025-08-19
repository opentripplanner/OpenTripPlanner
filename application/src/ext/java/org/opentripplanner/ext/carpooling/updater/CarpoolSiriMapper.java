package org.opentripplanner.ext.carpooling.updater;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.model.CarpoolTripBuilder;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.AreaStop;
import uk.org.siri.siri21.EstimatedCall;
import uk.org.siri.siri21.EstimatedVehicleJourney;

public class CarpoolSiriMapper {

  private static final String FEED_ID = "ENT";
  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
  private static final AtomicInteger COUNTER = new AtomicInteger(0);

  public CarpoolTrip mapSiriToCarpoolTrip(EstimatedVehicleJourney journey) {

    var calls = journey.getEstimatedCalls().getEstimatedCalls();
    if (calls.size() != 2) {
      throw new IllegalArgumentException("Carpool trips must have exactly 2 stops for now.");
    }

    var boardingCall = calls.getFirst();
    var alightingCall = calls.getLast();

    String lineRef = journey.getLineRef().getValue();
    String tripId = lineRef.substring(lineRef.lastIndexOf(':') + 1);

    AreaStop boardingArea = buildAreaStop(boardingCall, tripId + "_boarding");
    AreaStop alightingArea = buildAreaStop(alightingCall, tripId + "_alighting");

    ZonedDateTime startTime = boardingCall.getExpectedDepartureTime() != null
      ? boardingCall.getExpectedDepartureTime()
      : boardingCall.getAimedDepartureTime();

    ZonedDateTime endTime = alightingCall.getExpectedArrivalTime() != null
      ? alightingCall.getExpectedArrivalTime()
      : alightingCall.getAimedArrivalTime();

    String provider = journey.getOperatorRef().getValue();

    return new CarpoolTripBuilder(new FeedScopedId(FEED_ID, tripId))
      .withBoardingArea(boardingArea)
      .withAlightingArea(alightingArea)
      .withStartTime(startTime)
      .withEndTime(endTime)
      .withProvider(provider)
      .withAvailableSeats(1) // Default value, could be enhanced if data available
      .build();
  }

  private AreaStop buildAreaStop(EstimatedCall call, String id) {
    var stopAssignments = call.getDepartureStopAssignments();
    if (stopAssignments == null || stopAssignments.size() != 1) {
      throw new IllegalArgumentException("Expected exactly one stop assignment for call: " + call);
    }
    var flexibleArea = stopAssignments.getFirst()
      .getExpectedFlexibleArea();

    if (flexibleArea == null || flexibleArea.getPolygon() == null) {
      throw new IllegalArgumentException("Missing flexible area for stop");
    }

    var polygon = createPolygonFromGml(flexibleArea.getPolygon());

    return AreaStop.of(new FeedScopedId(FEED_ID, id), COUNTER::getAndIncrement)
      .withName(I18NString.of(call.getStopPointNames().getFirst().getValue()))
      .withGeometry(polygon)
      .build();
  }

  private Polygon createPolygonFromGml(net.opengis.gml._3.PolygonType gmlPolygon) {
    var abstractRing = gmlPolygon.getExterior()
      .getAbstractRing()
      .getValue();

    if (!(abstractRing instanceof net.opengis.gml._3.LinearRingType)) {
      throw new IllegalArgumentException("Expected LinearRingType for polygon exterior");
    }

    var linearRing = (net.opengis.gml._3.LinearRingType) abstractRing;
    List<Double> values = linearRing.getPosList().getValue();

    // Convert to JTS coordinates (lon lat pairs)
    Coordinate[] coords = new Coordinate[values.size() / 2];
    for (int i = 0; i < values.size(); i += 2) {
      coords[i / 2] = new Coordinate(values.get(i), values.get(i + 1));
    }

    LinearRing shell = GEOMETRY_FACTORY.createLinearRing(coords);
    return GEOMETRY_FACTORY.createPolygon(shell);
  }
}