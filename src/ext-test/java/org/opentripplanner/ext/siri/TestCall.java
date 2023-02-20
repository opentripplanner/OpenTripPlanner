package org.opentripplanner.ext.siri;

import java.time.ZonedDateTime;
import java.util.List;
import uk.org.siri.siri20.ArrivalBoardingActivityEnumeration;
import uk.org.siri.siri20.CallStatusEnumeration;
import uk.org.siri.siri20.DepartureBoardingActivityEnumeration;
import uk.org.siri.siri20.NaturalLanguageStringStructure;
import uk.org.siri.siri20.OccupancyEnumeration;

public class TestCall implements CallWrapper {

  private final String stopPointRef;
  private final Boolean cancellation;
  private final ZonedDateTime aimedArrivalTime;
  private final ZonedDateTime expectedArrivalTime;
  private final ZonedDateTime actualArrivalTime;
  private final ZonedDateTime aimedDepartureTime;
  private final ZonedDateTime expectedDepartureTime;
  private final ZonedDateTime actualDepartureTime;

  private TestCall(
    String stopPointRef,
    ZonedDateTime aimedArrivalTime,
    ZonedDateTime expectedArrivalTime,
    ZonedDateTime actualArrivalTime,
    ZonedDateTime aimedDepartureTime,
    ZonedDateTime expectedDepartureTime,
    ZonedDateTime actualDepartureTime
  ) {
    this.stopPointRef = stopPointRef;
    this.cancellation = false;
    this.aimedArrivalTime = aimedArrivalTime;
    this.expectedArrivalTime = expectedArrivalTime;
    this.actualArrivalTime = actualArrivalTime;
    this.aimedDepartureTime = aimedDepartureTime;
    this.expectedDepartureTime = expectedDepartureTime;
    this.actualDepartureTime = actualDepartureTime;
  }

  public static TestCall from(
    String stopPointRef,
    ZonedDateTime aimedDepartureTime,
    ZonedDateTime expectedDepartureTime,
    ZonedDateTime actualDepartureTime
  ) {
    return new TestCall(
      stopPointRef,
      null,
      null,
      null,
      aimedDepartureTime,
      expectedDepartureTime,
      actualDepartureTime
    );
  }

  public static TestCall intermediate(
    String stopPointRef,
    ZonedDateTime aimedArrivalTime,
    ZonedDateTime expectedArrivalTime,
    ZonedDateTime actualArrivalTime,
    ZonedDateTime aimedDepartureTime,
    ZonedDateTime expectedDepartureTime,
    ZonedDateTime actualDepartureTime
  ) {
    return new TestCall(
      stopPointRef,
      aimedArrivalTime,
      expectedArrivalTime,
      actualArrivalTime,
      aimedDepartureTime,
      expectedDepartureTime,
      actualDepartureTime
    );
  }

  public static TestCall to(
    String stopPointRef,
    ZonedDateTime aimedArrivalTime,
    ZonedDateTime expectedArrivalTime,
    ZonedDateTime actualArrivalTime
  ) {
    return new TestCall(
      stopPointRef,
      aimedArrivalTime,
      expectedArrivalTime,
      actualArrivalTime,
      null,
      null,
      null
    );
  }

  @Override
  public String getStopPointRef() {
    return stopPointRef;
  }

  @Override
  public Boolean isCancellation() {
    return cancellation;
  }

  @Override
  public Boolean isPredictionInaccurate() {
    return null;
  }

  @Override
  public OccupancyEnumeration getOccupancy() {
    return null;
  }

  @Override
  public List<NaturalLanguageStringStructure> getDestinationDisplaies() {
    return null;
  }

  @Override
  public ZonedDateTime getAimedArrivalTime() {
    return aimedArrivalTime;
  }

  @Override
  public ZonedDateTime getExpectedArrivalTime() {
    return expectedArrivalTime;
  }

  @Override
  public ZonedDateTime getActualArrivalTime() {
    return actualArrivalTime;
  }

  @Override
  public CallStatusEnumeration getArrivalStatus() {
    return null;
  }

  @Override
  public ArrivalBoardingActivityEnumeration getArrivalBoardingActivity() {
    return null;
  }

  @Override
  public ZonedDateTime getAimedDepartureTime() {
    return aimedDepartureTime;
  }

  @Override
  public ZonedDateTime getExpectedDepartureTime() {
    return expectedDepartureTime;
  }

  @Override
  public ZonedDateTime getActualDepartureTime() {
    return actualDepartureTime;
  }

  @Override
  public CallStatusEnumeration getDepartureStatus() {
    return null;
  }

  @Override
  public DepartureBoardingActivityEnumeration getDepartureBoardingActivity() {
    return null;
  }
}
