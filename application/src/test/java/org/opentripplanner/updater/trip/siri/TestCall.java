package org.opentripplanner.updater.trip.siri;

import java.time.ZonedDateTime;
import java.util.List;
import uk.org.siri.siri21.ArrivalBoardingActivityEnumeration;
import uk.org.siri.siri21.CallStatusEnumeration;
import uk.org.siri.siri21.DepartureBoardingActivityEnumeration;
import uk.org.siri.siri21.NaturalLanguageStringStructure;
import uk.org.siri.siri21.OccupancyEnumeration;

public class TestCall implements CallWrapper {

  private final String stopPointRef;
  private final Boolean cancellation;
  private final Boolean predictionInaccurate;
  private final OccupancyEnumeration occupancy;
  private final List<NaturalLanguageStringStructure> destinationDisplaies;
  private final ZonedDateTime aimedArrivalTime;
  private final ZonedDateTime expectedArrivalTime;
  private final ZonedDateTime actualArrivalTime;
  private final CallStatusEnumeration arrivalStatus;
  private final ArrivalBoardingActivityEnumeration arrivalBoardingActivity;
  private final ZonedDateTime aimedDepartureTime;
  private final ZonedDateTime expectedDepartureTime;
  private final ZonedDateTime actualDepartureTime;
  private final CallStatusEnumeration departureStatus;
  private final DepartureBoardingActivityEnumeration departureBoardingActivity;

  private TestCall(
    String stopPointRef,
    Boolean cancellation,
    Boolean predictionInaccurate,
    OccupancyEnumeration occupancy,
    List<NaturalLanguageStringStructure> destinationDisplaies,
    ZonedDateTime aimedArrivalTime,
    ZonedDateTime expectedArrivalTime,
    ZonedDateTime actualArrivalTime,
    CallStatusEnumeration arrivalStatus,
    ArrivalBoardingActivityEnumeration arrivalBoardingActivity,
    ZonedDateTime aimedDepartureTime,
    ZonedDateTime expectedDepartureTime,
    ZonedDateTime actualDepartureTime,
    CallStatusEnumeration departureStatus,
    DepartureBoardingActivityEnumeration departureBoardingActivity
  ) {
    this.stopPointRef = stopPointRef;
    this.cancellation = cancellation;
    this.predictionInaccurate = predictionInaccurate;
    this.occupancy = occupancy;
    this.destinationDisplaies = destinationDisplaies;
    this.aimedArrivalTime = aimedArrivalTime;
    this.expectedArrivalTime = expectedArrivalTime;
    this.actualArrivalTime = actualArrivalTime;
    this.arrivalStatus = arrivalStatus;
    this.arrivalBoardingActivity = arrivalBoardingActivity;
    this.aimedDepartureTime = aimedDepartureTime;
    this.expectedDepartureTime = expectedDepartureTime;
    this.actualDepartureTime = actualDepartureTime;
    this.departureStatus = departureStatus;
    this.departureBoardingActivity = departureBoardingActivity;
  }

  public static TestCallBuilder of() {
    return new TestCallBuilder();
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
    return predictionInaccurate;
  }

  @Override
  public OccupancyEnumeration getOccupancy() {
    return occupancy;
  }

  @Override
  public List<NaturalLanguageStringStructure> getDestinationDisplaies() {
    return destinationDisplaies;
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
    return arrivalStatus;
  }

  @Override
  public ArrivalBoardingActivityEnumeration getArrivalBoardingActivity() {
    return arrivalBoardingActivity;
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
    return departureStatus;
  }

  @Override
  public DepartureBoardingActivityEnumeration getDepartureBoardingActivity() {
    return departureBoardingActivity;
  }

  public static class TestCallBuilder {

    private String stopPointRef = null;
    private Boolean cancellation = null;
    private Boolean predictionInaccurate = null;
    private OccupancyEnumeration occupancy = null;
    private List<NaturalLanguageStringStructure> destinationDisplaies = null;
    private ZonedDateTime aimedArrivalTime = null;
    private ZonedDateTime expectedArrivalTime = null;
    private ZonedDateTime actualArrivalTime = null;
    private CallStatusEnumeration arrivalStatus = null;
    private ArrivalBoardingActivityEnumeration arrivalBoardingActivity = null;
    private ZonedDateTime aimedDepartureTime = null;
    private ZonedDateTime expectedDepartureTime = null;
    private ZonedDateTime actualDepartureTime = null;
    private CallStatusEnumeration departureStatus = null;
    private DepartureBoardingActivityEnumeration departureBoardingActivity = null;

    public TestCallBuilder withStopPointRef(String stopPointRef) {
      this.stopPointRef = stopPointRef;
      return this;
    }

    public TestCallBuilder withCancellation(Boolean cancellation) {
      this.cancellation = cancellation;
      return this;
    }

    public TestCallBuilder withPredictionInaccurate(Boolean predictionInaccurate) {
      this.predictionInaccurate = predictionInaccurate;
      return this;
    }

    public TestCallBuilder withOccupancy(OccupancyEnumeration occupancy) {
      this.occupancy = occupancy;
      return this;
    }

    public TestCallBuilder withDestinationDisplaies(
      List<NaturalLanguageStringStructure> destinationDisplaies
    ) {
      this.destinationDisplaies = destinationDisplaies;
      return this;
    }

    public TestCallBuilder withAimedArrivalTime(ZonedDateTime aimedArrivalTime) {
      this.aimedArrivalTime = aimedArrivalTime;
      return this;
    }

    public TestCallBuilder withExpectedArrivalTime(ZonedDateTime expectedArrivalTime) {
      this.expectedArrivalTime = expectedArrivalTime;
      return this;
    }

    public TestCallBuilder withActualArrivalTime(ZonedDateTime actualArrivalTime) {
      this.actualArrivalTime = actualArrivalTime;
      return this;
    }

    public TestCallBuilder withArrivalStatus(CallStatusEnumeration arrivalStatus) {
      this.arrivalStatus = arrivalStatus;
      return this;
    }

    public TestCallBuilder withArrivalBoardingActivity(
      ArrivalBoardingActivityEnumeration arrivalBoardingActivity
    ) {
      this.arrivalBoardingActivity = arrivalBoardingActivity;
      return this;
    }

    public TestCallBuilder withAimedDepartureTime(ZonedDateTime aimedDepartureTime) {
      this.aimedDepartureTime = aimedDepartureTime;
      return this;
    }

    public TestCallBuilder withExpectedDepartureTime(ZonedDateTime expectedDepartureTime) {
      this.expectedDepartureTime = expectedDepartureTime;
      return this;
    }

    public TestCallBuilder withActualDepartureTime(ZonedDateTime actualDepartureTime) {
      this.actualDepartureTime = actualDepartureTime;
      return this;
    }

    public TestCallBuilder withDepartureStatus(CallStatusEnumeration departureStatus) {
      this.departureStatus = departureStatus;
      return this;
    }

    public TestCallBuilder withDepartureBoardingActivity(
      DepartureBoardingActivityEnumeration departureBoardingActivity
    ) {
      this.departureBoardingActivity = departureBoardingActivity;
      return this;
    }

    public TestCall build() {
      return new TestCall(
        stopPointRef,
        cancellation,
        predictionInaccurate,
        occupancy,
        destinationDisplaies,
        aimedArrivalTime,
        expectedArrivalTime,
        actualArrivalTime,
        arrivalStatus,
        arrivalBoardingActivity,
        aimedDepartureTime,
        expectedDepartureTime,
        actualDepartureTime,
        departureStatus,
        departureBoardingActivity
      );
    }
  }
}
