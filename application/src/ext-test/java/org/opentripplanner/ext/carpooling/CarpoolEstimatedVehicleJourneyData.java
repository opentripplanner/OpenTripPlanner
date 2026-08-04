package org.opentripplanner.ext.carpooling;

import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_EAST;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_WEST;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.Arrays;
import net.opengis.gml.siri.AbstractRingPropertyType;
import net.opengis.gml.siri.LinearRingType;
import net.opengis.gml.siri.ObjectFactory;
import net.opengis.gml.siri.PolygonType;
import net.opengis.gml.siri.PosList;
import org.opentripplanner.street.geometry.WgsCoordinate;
import uk.org.siri.siri21.AimedFlexibleArea;
import uk.org.siri.siri21.CircularAreaStructure;
import uk.org.siri.siri21.EstimatedCall;
import uk.org.siri.siri21.EstimatedVehicleJourney;
import uk.org.siri.siri21.NaturalLanguageStringStructure;
import uk.org.siri.siri21.OperatorRefStructure;
import uk.org.siri.siri21.PassengerCapacityStructure;
import uk.org.siri.siri21.SimpleContactStructure;
import uk.org.siri.siri21.StopAssignmentStructure;
import uk.org.siri.siri21.VehicleOccupancyStructure;

public class CarpoolEstimatedVehicleJourneyData {

  private static final String FIRST_STOP_POLYGON =
    "10.813864907662264 59.904961620490184 10.818271230878196 59.903856857974404 10.818498026925795 59.90393809176436 10.818604482213118 59.9039055982723 10.818386943147004 59.90379187079944 10.818215688988204 59.90380579663352 10.81386953615231 59.90490591905822 10.813864907662264 59.904961620490184";
  private static final String LAST_STOP_POLYGON =
    "10.197976677739746 59.74492585818382 10.19775667855555 59.74465335895027 10.197230513839912 59.74475958772939 10.19714251416596 59.74464319791892 10.197628345697694 59.744487087140186 10.197844678229416 59.744667214897106 10.198856674477867 59.74438085749492 10.19892634088606 59.744463993767596 10.198640341946088 59.74453696877012 10.19883100790608 59.74475404536645 10.197976677739746 59.74492585818382";

  public static EstimatedVehicleJourney arrivalIsAfterDepartureTime() {
    var journey = new EstimatedVehicleJourney();
    journey.setEstimatedCalls(new EstimatedVehicleJourney.EstimatedCalls());
    var first = new EstimatedCall();
    first.setAimedDepartureTime(ZonedDateTime.now().plusDays(1));
    var last = new EstimatedCall();
    last.setAimedArrivalTime(ZonedDateTime.now());
    journey.getEstimatedCalls().getEstimatedCalls().add(first);
    journey.getEstimatedCalls().getEstimatedCalls().add(last);

    return journey;
  }

  public static EstimatedVehicleJourney lessThanTwoStops() {
    var journey = new EstimatedVehicleJourney();
    journey.setEstimatedCalls(new EstimatedVehicleJourney.EstimatedCalls());
    journey.getEstimatedCalls().getEstimatedCalls().add(new EstimatedCall());

    return journey;
  }

  public static EstimatedVehicleJourney minimalCompleteJourney() {
    var journey = new EstimatedVehicleJourney();
    var operator = new OperatorRefStructure();
    operator.setValue("ENT");
    journey.setEstimatedVehicleJourneyCode("unittest");
    journey.setOperatorRef(operator);

    var firstStop = forPoint(OSLO_EAST);
    firstStop.setAimedDepartureTime(ZonedDateTime.now());
    var firstName = new NaturalLanguageStringStructure();
    firstName.setValue("First stop");
    firstStop.getStopPointNames().add(firstName);
    var lastStop = forPoint(OSLO_NORTH);
    lastStop.setAimedDepartureTime(ZonedDateTime.now());
    var lastName = new NaturalLanguageStringStructure();
    lastName.setValue("Last stop");
    lastStop.getStopPointNames().add(lastName);
    lastStop.setAimedArrivalTime(ZonedDateTime.now().plusMinutes(45));

    journey.setEstimatedCalls(new EstimatedVehicleJourney.EstimatedCalls());
    journey.getEstimatedCalls().getEstimatedCalls().add(firstStop);
    journey.getEstimatedCalls().getEstimatedCalls().add(lastStop);

    return journey;
  }

  public static EstimatedVehicleJourney minimalCompleteJourneyWithPolygon() {
    var journey = new EstimatedVehicleJourney();
    var operator = new OperatorRefStructure();
    operator.setValue("ENT");
    journey.setEstimatedVehicleJourneyCode("unittest");
    journey.setOperatorRef(operator);

    var firstStop = forPolygon(FIRST_STOP_POLYGON);
    firstStop.setAimedDepartureTime(ZonedDateTime.now());
    var firstName = new NaturalLanguageStringStructure();
    firstName.setValue("First stop");
    firstStop.getStopPointNames().add(firstName);

    var lastStop = forPolygon(LAST_STOP_POLYGON);
    lastStop.setAimedDepartureTime(ZonedDateTime.now());
    var lastName = new NaturalLanguageStringStructure();
    lastName.setValue("Last stop");
    lastStop.getStopPointNames().add(lastName);
    lastStop.setAimedArrivalTime(ZonedDateTime.now().plusMinutes(45));

    journey.setEstimatedCalls(new EstimatedVehicleJourney.EstimatedCalls());
    journey.getEstimatedCalls().getEstimatedCalls().add(firstStop);
    journey.getEstimatedCalls().getEstimatedCalls().add(lastStop);

    return journey;
  }

  public static EstimatedVehicleJourney tripHasAimedTimesOnly() {
    var journey = minimalCompleteJourney();

    var firstStop = journey.getEstimatedCalls().getEstimatedCalls().getFirst();
    var lastStop = journey.getEstimatedCalls().getEstimatedCalls().getLast();

    firstStop.setAimedDepartureTime(ZonedDateTime.now());
    firstStop.setExpectedDepartureTime(null);
    lastStop.setAimedArrivalTime(ZonedDateTime.now().plusMinutes(45));
    lastStop.setExpectedArrivalTime(null);

    return journey;
  }

  public static EstimatedVehicleJourney tripHasExpectedTimesOnly() {
    var journey = minimalCompleteJourney();

    var firstStop = journey.getEstimatedCalls().getEstimatedCalls().getFirst();
    var lastStop = journey.getEstimatedCalls().getEstimatedCalls().getLast();

    firstStop.setAimedDepartureTime(null);
    firstStop.setExpectedDepartureTime(ZonedDateTime.now());
    lastStop.setAimedArrivalTime(null);
    lastStop.setExpectedArrivalTime(ZonedDateTime.now().plusMinutes(45));

    return journey;
  }

  /**
   * Aimed times are absent, so call-order validation cannot compare them — the inverted timeline
   * is only visible on the derived expected start/end times.
   */
  public static EstimatedVehicleJourney expectedArrivalBeforeExpectedDeparture() {
    var journey = minimalCompleteJourney();

    var firstStop = journey.getEstimatedCalls().getEstimatedCalls().getFirst();
    var lastStop = journey.getEstimatedCalls().getEstimatedCalls().getLast();

    firstStop.setAimedDepartureTime(null);
    firstStop.setExpectedDepartureTime(ZonedDateTime.now().plusMinutes(45));
    lastStop.setAimedArrivalTime(null);
    lastStop.setExpectedArrivalTime(ZonedDateTime.now());

    return journey;
  }

  public static EstimatedVehicleJourney journeyWithPublicContact(String phoneNumber, String url) {
    var journey = minimalCompleteJourney();
    var contact = new SimpleContactStructure();
    contact.setPhoneNumber(phoneNumber);
    contact.setUrl(url);
    journey.setPublicContact(contact);
    return journey;
  }

  /**
   * A minimal complete journey flagged as cancelled at the trip level.
   */
  public static EstimatedVehicleJourney cancelledJourney() {
    var journey = minimalCompleteJourney();
    journey.setCancellation(true);
    return journey;
  }

  /**
   * A 3-stop journey where the intermediate call is flagged as cancelled — the driver still
   * drives origin → destination but skips the middle waypoint.
   */
  public static EstimatedVehicleJourney journeyWithCancelledIntermediateCall() {
    var journey = minimalCompleteJourney();
    var calls = journey.getEstimatedCalls().getEstimatedCalls();
    var base = calls.getFirst().getAimedDepartureTime();

    var intermediate = forPoint(OSLO_NORTH);
    intermediate.setAimedArrivalTime(base.plusMinutes(20));
    intermediate.setAimedDepartureTime(base.plusMinutes(20));
    intermediate.setCancellation(true);
    addStopName(intermediate, "Cancelled intermediate");

    calls.add(1, intermediate);
    return journey;
  }

  /**
   * A 3-stop journey where two of three calls are cancelled, leaving fewer than 2 active calls.
   */
  public static EstimatedVehicleJourney journeyWithAllButOneCallCancelled() {
    var journey = journeyWithCancelledIntermediateCall();
    journey.getEstimatedCalls().getEstimatedCalls().getLast().setCancellation(true);
    return journey;
  }

  /**
   * Same trip id as {@link #minimalCompleteJourney()} but with the last call's aimed arrival
   * time set before the first call's aimed departure time, causing the mapper to throw during
   * call-order validation. Not flagged as cancelled.
   */
  public static EstimatedVehicleJourney malformedNonCancelledJourney() {
    var journey = minimalCompleteJourney();
    var calls = journey.getEstimatedCalls().getEstimatedCalls();
    var firstDeparture = calls.getFirst().getAimedDepartureTime();
    calls.getLast().setAimedArrivalTime(firstDeparture.minusMinutes(45));
    return journey;
  }

  /**
   * Like {@link #minimalCompleteJourney()} but with the destination moved, giving a changed
   * route-point geometry.
   */
  public static EstimatedVehicleJourney journeyWithMovedDestination() {
    var journey = minimalCompleteJourney();
    var calls = journey.getEstimatedCalls().getEstimatedCalls();
    var original = calls.getLast();
    var moved = forPoint(OSLO_WEST);
    moved.setAimedDepartureTime(original.getAimedDepartureTime());
    moved.setAimedArrivalTime(original.getAimedArrivalTime());
    addStopName(moved, "Moved last stop");
    calls.set(calls.size() - 1, moved);
    return journey;
  }

  public static EstimatedVehicleJourney stopTimesAreOutOfOrder() {
    var journey = new EstimatedVehicleJourney();
    journey.setEstimatedCalls(new EstimatedVehicleJourney.EstimatedCalls());
    var first = new EstimatedCall();
    first.setAimedDepartureTime(ZonedDateTime.now());
    var middle = new EstimatedCall();
    middle.setAimedArrivalTime(ZonedDateTime.now().plusDays(1));
    var last = new EstimatedCall();
    last.setAimedArrivalTime(ZonedDateTime.now());
    journey.getEstimatedCalls().getEstimatedCalls().add(first);
    journey.getEstimatedCalls().getEstimatedCalls().add(middle);
    journey.getEstimatedCalls().getEstimatedCalls().add(last);

    return journey;
  }

  static EstimatedCall forPoint(WgsCoordinate coordinate) {
    var call = new EstimatedCall();
    call.setAimedDepartureTime(ZonedDateTime.now());
    var circularArea = new CircularAreaStructure();
    circularArea.setLatitude(BigDecimal.valueOf(coordinate.latitude()));
    circularArea.setLongitude(BigDecimal.valueOf(coordinate.longitude()));
    circularArea.setRadius(BigInteger.valueOf(1));
    var stop = new StopAssignmentStructure();
    var flexibleArea = new AimedFlexibleArea();
    flexibleArea.setCircularArea(circularArea);
    stop.setExpectedFlexibleArea(flexibleArea);
    call.getDepartureStopAssignments().add(stop);

    return call;
  }

  static EstimatedCall forPolygon(String posList) {
    var call = new EstimatedCall();
    call.setAimedDepartureTime(ZonedDateTime.now());

    var stop = new StopAssignmentStructure();
    var flexibleStop = poslistToAimedFlexibleArea(posList);
    stop.setExpectedFlexibleArea(flexibleStop);

    call.getDepartureStopAssignments().add(stop);
    return call;
  }

  public static EstimatedVehicleJourney journeyWithTotalCapacity(int capacity) {
    var journey = minimalCompleteJourney();
    for (var call : journey.getEstimatedCalls().getEstimatedCalls()) {
      addTotalCapacity(call, capacity);
    }
    return journey;
  }

  public static EstimatedVehicleJourney journeyWithDifferentCapacitiesPerCall(
    int firstCapacity,
    int lastCapacity
  ) {
    var journey = minimalCompleteJourney();
    var calls = journey.getEstimatedCalls().getEstimatedCalls();
    addTotalCapacity(calls.getFirst(), firstCapacity);
    addTotalCapacity(calls.getLast(), lastCapacity);
    return journey;
  }

  public static EstimatedVehicleJourney journeyWithOnboardCounts(int... onboardCounts) {
    var journey = minimalCompleteJourney();
    var calls = journey.getEstimatedCalls().getEstimatedCalls();
    for (int i = 0; i < Math.min(onboardCounts.length, calls.size()); i++) {
      addOnboardCount(calls.get(i), onboardCounts[i]);
    }
    return journey;
  }

  private static void addTotalCapacity(EstimatedCall call, int totalCapacity) {
    var capacity = new PassengerCapacityStructure();
    capacity.setTotalCapacity(BigInteger.valueOf(totalCapacity));
    call.getExpectedDepartureCapacities().add(capacity);
  }

  public static EstimatedVehicleJourney journeyWithLatestExpectedArrivalTime(
    int expectedArrivalMinutes,
    int latestExpectedArrivalMinutes
  ) {
    var journey = minimalCompleteJourney();
    var lastStop = journey.getEstimatedCalls().getEstimatedCalls().getLast();
    var base = lastStop.getAimedArrivalTime();
    lastStop.setExpectedArrivalTime(base.plusMinutes(expectedArrivalMinutes));
    lastStop.setLatestExpectedArrivalTime(base.plusMinutes(latestExpectedArrivalMinutes));
    return journey;
  }

  public static EstimatedVehicleJourney journeyWithLatestExpectedArrivalTimeAimedOnly(
    int latestExpectedArrivalMinutes
  ) {
    var journey = minimalCompleteJourney();
    var lastStop = journey.getEstimatedCalls().getEstimatedCalls().getLast();
    var base = lastStop.getAimedArrivalTime();
    lastStop.setExpectedArrivalTime(null);
    lastStop.setLatestExpectedArrivalTime(base.plusMinutes(latestExpectedArrivalMinutes));
    return journey;
  }

  /**
   * A two-stop journey whose destination latest-expected arrival is 3 hours after departure —
   * beyond the mapper's 2.5-hour maximum trip duration — used to exercise the max-duration
   * safeguard. The scheduled arrival stays short, so only the latest-expected arrival pushes the
   * span over the limit.
   */
  public static EstimatedVehicleJourney tripExceedingMaxDuration() {
    var journey = minimalCompleteJourney();
    var calls = journey.getEstimatedCalls().getEstimatedCalls();
    var departure = calls.getFirst().getAimedDepartureTime();
    calls.getLast().setLatestExpectedArrivalTime(departure.plusHours(3));
    return journey;
  }

  /**
   * A two-stop journey with no latest-expected arrival whose scheduled arrival (2h20m after
   * departure) is within the 2.5-hour limit, but exceeds it once the default 15-minute deviation
   * budget is added — exercising the safeguard's deviation-budget fallback.
   */
  public static EstimatedVehicleJourney tripExceedingMaxDurationViaDefaultDeviationBudget() {
    var journey = minimalCompleteJourney();
    var calls = journey.getEstimatedCalls().getEstimatedCalls();
    var departure = calls.getFirst().getAimedDepartureTime();
    calls.getLast().setAimedArrivalTime(departure.plusMinutes(140));
    return journey;
  }

  /**
   * A two-stop journey whose claimed schedule is short (well within the limit) but whose waypoints
   * lie hundreds of kilometres apart, so the straight-line drive time exceeds the maximum trip
   * duration even at the fastest modelled car speed. Exercises the geometry safeguard, which the
   * timetable check cannot catch: a malformed feed can claim any times it likes regardless of how
   * far apart the coordinates actually are.
   */
  public static EstimatedVehicleJourney tripWithWaypointsTooFarApart() {
    var journey = minimalCompleteJourney();
    var calls = journey.getEstimatedCalls().getEstimatedCalls();
    var departure = calls.getFirst().getAimedDepartureTime();

    // ~450 km north of the Oslo origin: unreachable within 2.5 h even at the maximum modelled car
    // speed, yet the claimed arrival stays a plausible 45 minutes out.
    var farStop = forPoint(new WgsCoordinate(64.0, 10.81));
    farStop.setAimedDepartureTime(departure);
    farStop.setAimedArrivalTime(departure.plusMinutes(45));
    addStopName(farStop, "Far stop");
    calls.set(1, farStop);

    return journey;
  }

  /**
   * Builds a 3-stop journey (origin, intermediate, destination) where the intermediate and
   * destination stops each get their own {@code expectedArrivalTime} and
   * {@code latestExpectedArrivalTime}, enabling assertions on per-stop deviation budgets.
   * Arrival times are offset from {@code now} in minutes.
   */
  public static EstimatedVehicleJourney journeyWithPerStopLatestExpectedArrivalTimes(
    int intermediateExpectedArrivalMinutes,
    int intermediateLatestExpectedArrivalMinutes,
    int lastExpectedArrivalMinutes,
    int lastLatestExpectedArrivalMinutes
  ) {
    var base = ZonedDateTime.now();

    var origin = forPoint(OSLO_EAST);
    origin.setAimedDepartureTime(base);
    addStopName(origin, "Origin");

    var intermediate = createArrivalStop(
      OSLO_NORTH,
      "Intermediate",
      base,
      intermediateExpectedArrivalMinutes,
      intermediateLatestExpectedArrivalMinutes
    );
    intermediate.setAimedDepartureTime(base.plusMinutes(intermediateExpectedArrivalMinutes));

    var last = createArrivalStop(
      OSLO_NORTH,
      "Last",
      base,
      lastExpectedArrivalMinutes,
      lastLatestExpectedArrivalMinutes
    );

    var journey = new EstimatedVehicleJourney();
    var operator = new OperatorRefStructure();
    operator.setValue("TESTOPERATOR");
    journey.setEstimatedVehicleJourneyCode("unittest");
    journey.setOperatorRef(operator);
    journey.setEstimatedCalls(new EstimatedVehicleJourney.EstimatedCalls());
    journey.getEstimatedCalls().getEstimatedCalls().add(origin);
    journey.getEstimatedCalls().getEstimatedCalls().add(intermediate);
    journey.getEstimatedCalls().getEstimatedCalls().add(last);

    return journey;
  }

  private static EstimatedCall createArrivalStop(
    WgsCoordinate coordinate,
    String name,
    ZonedDateTime base,
    int expectedArrivalMinutes,
    int latestExpectedArrivalMinutes
  ) {
    var arrivalTime = base.plusMinutes(expectedArrivalMinutes);
    var call = forPoint(coordinate);
    call.setAimedArrivalTime(arrivalTime);
    call.setExpectedArrivalTime(arrivalTime);
    call.setLatestExpectedArrivalTime(base.plusMinutes(latestExpectedArrivalMinutes));
    addStopName(call, name);
    return call;
  }

  private static void addStopName(EstimatedCall call, String name) {
    var nameStruct = new NaturalLanguageStringStructure();
    nameStruct.setValue(name);
    call.getStopPointNames().add(nameStruct);
  }

  private static void addOnboardCount(EstimatedCall call, int onboardCount) {
    var occupancy = new VehicleOccupancyStructure();
    occupancy.setOnboardCount(BigInteger.valueOf(onboardCount));
    call.getExpectedDepartureOccupancies().add(occupancy);
  }

  static AimedFlexibleArea poslistToAimedFlexibleArea(String coordinates) {
    var gmlFactory = new ObjectFactory();

    var posListValues = Arrays.stream(coordinates.trim().split("\\s+"))
      .map(Double::valueOf)
      .toList();
    var posList = new PosList();
    posList.getValues().addAll(posListValues);
    var linearRing = new LinearRingType();
    linearRing.setPosList(posList);
    var abstractRing = new AbstractRingPropertyType();
    abstractRing.setAbstractRing(gmlFactory.createLinearRing(linearRing));
    var polygon = new PolygonType();
    polygon.setExterior(abstractRing);

    var area = new AimedFlexibleArea();
    area.setPolygon(polygon);
    return area;
  }
}
