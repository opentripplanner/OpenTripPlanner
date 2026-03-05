package org.opentripplanner.ext.carpooling;

import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_EAST;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import uk.org.siri.siri21.AimedFlexibleArea;
import uk.org.siri.siri21.CircularAreaStructure;
import uk.org.siri.siri21.EstimatedCall;
import uk.org.siri.siri21.EstimatedVehicleJourney;
import uk.org.siri.siri21.NaturalLanguageStringStructure;
import uk.org.siri.siri21.OperatorRefStructure;
import uk.org.siri.siri21.StopAssignmentStructure;

public class CarpoolEstimatedVehicleJourneyData {

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
}
