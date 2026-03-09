package org.opentripplanner.ext.carpooling;

import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_EAST;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.Arrays;
import net.opengis.gml._3.AbstractRingPropertyType;
import net.opengis.gml._3.DirectPositionListType;
import net.opengis.gml._3.LinearRingType;
import net.opengis.gml._3.ObjectFactory;
import net.opengis.gml._3.PolygonType;
import org.opentripplanner.street.geometry.WgsCoordinate;
import uk.org.siri.siri21.AimedFlexibleArea;
import uk.org.siri.siri21.CircularAreaStructure;
import uk.org.siri.siri21.EstimatedCall;
import uk.org.siri.siri21.EstimatedVehicleJourney;
import uk.org.siri.siri21.NaturalLanguageStringStructure;
import uk.org.siri.siri21.OperatorRefStructure;
import uk.org.siri.siri21.StopAssignmentStructure;

public class CarpoolEstimatedVehicleJourneyData {
  private static final String FIRST_STOP_POLYGON = "10.813864907662264 59.904961620490184 10.818271230878196 59.903856857974404 10.818498026925795 59.90393809176436 10.818604482213118 59.9039055982723 10.818386943147004 59.90379187079944 10.818215688988204 59.90380579663352 10.81386953615231 59.90490591905822 10.813864907662264 59.904961620490184";
  private static final String LAST_STOP_POLYGON = "10.197976677739746 59.74492585818382 10.19775667855555 59.74465335895027 10.197230513839912 59.74475958772939 10.19714251416596 59.74464319791892 10.197628345697694 59.744487087140186 10.197844678229416 59.744667214897106 10.198856674477867 59.74438085749492 10.19892634088606 59.744463993767596 10.198640341946088 59.74453696877012 10.19883100790608 59.74475404536645 10.197976677739746 59.74492585818382";

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
    stop.setAimedFlexibleArea(flexibleStop);

    call.getDepartureStopAssignments().add(stop);
    return call;
  }

  static AimedFlexibleArea poslistToAimedFlexibleArea(String coordinates) {
    var gmlFactory = new ObjectFactory();

    var poslist = Arrays.stream(coordinates.trim().split("\\s+"))
      .map(Double::valueOf)
      .toList();
    var polygon = new PolygonType()
      .withExterior(new AbstractRingPropertyType()
        .withAbstractRing(gmlFactory.createLinearRing(new LinearRingType()
          .withPosList(new DirectPositionListType()
            .withValue(poslist)))));

    var area = new AimedFlexibleArea();
    area.setPolygon(polygon);
    return area;
  }
}
