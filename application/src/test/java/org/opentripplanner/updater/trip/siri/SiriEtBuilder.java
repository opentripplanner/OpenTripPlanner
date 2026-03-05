package org.opentripplanner.updater.trip.siri;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.opentripplanner.LocalTimeParser;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import uk.org.siri.siri21.ArrivalBoardingActivityEnumeration;
import uk.org.siri.siri21.CallStatusEnumeration;
import uk.org.siri.siri21.DataFrameRefStructure;
import uk.org.siri.siri21.DatedVehicleJourneyRef;
import uk.org.siri.siri21.DepartureBoardingActivityEnumeration;
import uk.org.siri.siri21.EstimatedCall;
import uk.org.siri.siri21.EstimatedTimetableDeliveryStructure;
import uk.org.siri.siri21.EstimatedVehicleJourney;
import uk.org.siri.siri21.EstimatedVersionFrameStructure;
import uk.org.siri.siri21.FramedVehicleJourneyRefStructure;
import uk.org.siri.siri21.LineRef;
import uk.org.siri.siri21.NaturalLanguageStringStructure;
import uk.org.siri.siri21.OccupancyEnumeration;
import uk.org.siri.siri21.OperatorRefStructure;
import uk.org.siri.siri21.QuayRefStructure;
import uk.org.siri.siri21.RecordedCall;
import uk.org.siri.siri21.StopAssignmentStructure;
import uk.org.siri.siri21.StopPointRefStructure;
import uk.org.siri.siri21.VehicleJourneyRef;
import uk.org.siri.siri21.VehicleModesEnumeration;
import uk.org.siri.siri21.VehicleRef;

/**
 * This is a helper class for constucting Siri ET messages to use in tests.
 */
public class SiriEtBuilder {

  private final EstimatedVehicleJourney evj;
  private final LocalTimeParser localTimeParser;

  public SiriEtBuilder(LocalTimeParser localTimeParser) {
    this.localTimeParser = localTimeParser;
    this.evj = new EstimatedVehicleJourney();

    // Set default values
    evj.setMonitored(true);
    evj.setDataSource("DATASOURCE");
  }

  public List<EstimatedTimetableDeliveryStructure> buildEstimatedTimetableDeliveries() {
    var versionFrame = new EstimatedVersionFrameStructure();
    versionFrame.getEstimatedVehicleJourneies().add(evj);

    var etd = new EstimatedTimetableDeliveryStructure();
    etd.getEstimatedJourneyVersionFrames().add(versionFrame);
    return List.of(etd);
  }

  public EstimatedVehicleJourney buildEstimatedVehicleJourney() {
    return evj;
  }

  public SiriEtBuilder withCancellation(boolean canceled) {
    evj.setCancellation(canceled);
    return this;
  }

  public SiriEtBuilder withMonitored(boolean monitored) {
    evj.setMonitored(monitored);
    return this;
  }

  public SiriEtBuilder withIsExtraJourney(boolean isExtraJourney) {
    evj.setExtraJourney(isExtraJourney);
    return this;
  }

  public SiriEtBuilder withDatedVehicleJourneyRef(String datedServiceJourneyId) {
    var ref = new DatedVehicleJourneyRef();
    ref.setValue(datedServiceJourneyId);
    evj.setDatedVehicleJourneyRef(ref);
    return this;
  }

  public SiriEtBuilder withEstimatedVehicleJourneyCode(String estimatedVehicleJourneyCode) {
    evj.setEstimatedVehicleJourneyCode(estimatedVehicleJourneyCode);
    return this;
  }

  public SiriEtBuilder withOperatorRef(String operatorRef) {
    var ref = new OperatorRefStructure();
    ref.setValue(operatorRef);
    evj.setOperatorRef(ref);
    return this;
  }

  public SiriEtBuilder withLineRef(String lineRef) {
    var ref = new LineRef();
    ref.setValue(lineRef);
    evj.setLineRef(ref);
    return this;
  }

  public SiriEtBuilder withOccupancy(OccupancyEnumeration occupancy) {
    evj.setOccupancy(occupancy);
    return this;
  }

  public SiriEtBuilder withPredictionInaccurate(boolean predictionInaccurate) {
    evj.setPredictionInaccurate(predictionInaccurate);
    return this;
  }

  public SiriEtBuilder withDestinationName(String destinationName) {
    var name = new NaturalLanguageStringStructure();
    name.setValue(destinationName);
    evj.getDestinationNames().add(name);
    return this;
  }

  public SiriEtBuilder withExternalLineRef(String externalLineRef) {
    var ref = new LineRef();
    ref.setValue(externalLineRef);
    evj.setExternalLineRef(ref);
    return this;
  }

  public SiriEtBuilder withPublishedLineName(String lineName) {
    var name = new NaturalLanguageStringStructure();
    name.setValue(lineName);
    evj.getPublishedLineNames().add(name);
    return this;
  }

  public SiriEtBuilder withVehicleRef(String vehicleRef) {
    var ref = new VehicleRef();
    ref.setValue(vehicleRef);
    evj.setVehicleRef(ref);
    return this;
  }

  public SiriEtBuilder withVehicleMode(VehicleModesEnumeration mode) {
    evj.getVehicleModes().add(mode);
    return this;
  }

  public SiriEtBuilder withRecordedCalls(
    Function<RecordedCallsBuilder, RecordedCallsBuilder> producer
  ) {
    if (evj.getEstimatedCalls() != null) {
      // If we call this after estimatedCalls, the ordering will be messed up
      throw new RuntimeException(
        "You need to call withRecordedCalls() before withEstimatedCalls()"
      );
    }
    var builder = new RecordedCallsBuilder(localTimeParser, 0);

    builder = producer.apply(builder);

    var calls = new EstimatedVehicleJourney.RecordedCalls();
    builder.build().forEach(call -> calls.getRecordedCalls().add(call));
    evj.setRecordedCalls(calls);
    return this;
  }

  public SiriEtBuilder withEstimatedCalls(
    Function<EstimatedCallsBuilder, EstimatedCallsBuilder> producer
  ) {
    int offset = evj.getRecordedCalls() == null
      ? 0
      : evj.getRecordedCalls().getRecordedCalls().size();
    var builder = new EstimatedCallsBuilder(localTimeParser, offset);

    builder = producer.apply(builder);

    var calls = new EstimatedVehicleJourney.EstimatedCalls();
    builder.build().forEach(call -> calls.getEstimatedCalls().add(call));
    evj.setEstimatedCalls(calls);
    return this;
  }

  public SiriEtBuilder withVehicleJourneyRef(String id) {
    var ref = new VehicleJourneyRef();
    ref.setValue(id);
    evj.setVehicleJourneyRef(ref);
    return this;
  }

  public SiriEtBuilder withFramedVehicleJourneyRef(
    Function<FramedVehicleRefBuilder, FramedVehicleRefBuilder> producer
  ) {
    var builder = new FramedVehicleRefBuilder();
    builder = producer.apply(builder);
    evj.setFramedVehicleJourneyRef(builder.build());
    return this;
  }

  public static class FramedVehicleRefBuilder {

    private LocalDate serviceDate;
    private String vehicleJourneyRef;

    public SiriEtBuilder.FramedVehicleRefBuilder withServiceDate(LocalDate serviceDate) {
      this.serviceDate = serviceDate;
      return this;
    }

    public SiriEtBuilder.FramedVehicleRefBuilder withVehicleJourneyRef(String vehicleJourneyRef) {
      this.vehicleJourneyRef = vehicleJourneyRef;
      return this;
    }

    public FramedVehicleJourneyRefStructure build() {
      DataFrameRefStructure dataFrameRefStructure = new DataFrameRefStructure();
      if (serviceDate != null) {
        dataFrameRefStructure.setValue(DateTimeFormatter.ISO_LOCAL_DATE.format(serviceDate));
      }
      FramedVehicleJourneyRefStructure framedVehicleJourneyRefStructure =
        new FramedVehicleJourneyRefStructure();
      framedVehicleJourneyRefStructure.setDataFrameRef(dataFrameRefStructure);
      framedVehicleJourneyRefStructure.setDatedVehicleJourneyRef(vehicleJourneyRef);
      return framedVehicleJourneyRefStructure;
    }
  }

  public static class RecordedCallsBuilder {

    private final ArrayList<RecordedCall> calls;
    private final int orderOffset;
    private final LocalTimeParser localTimeParser;

    public RecordedCallsBuilder(LocalTimeParser localTimeParser, int orderOffset) {
      this.localTimeParser = localTimeParser;
      this.orderOffset = orderOffset;
      this.calls = new ArrayList<>();
    }

    public RecordedCallsBuilder call(StopLocation stop) {
      var call = new RecordedCall();
      call.setOrder(BigInteger.valueOf(orderOffset + calls.size()));

      var ref = new StopPointRefStructure();
      ref.setValue(stop.getId().getId());
      call.setStopPointRef(ref);

      calls.add(call);
      return this;
    }

    public RecordedCallsBuilder arriveAimedActual(String aimedTime, String actualTime) {
      var call = calls.getLast();
      call.setAimedArrivalTime(localTimeParser.zonedDateTime(aimedTime));
      call.setActualArrivalTime(localTimeParser.zonedDateTime(actualTime));
      return this;
    }

    public RecordedCallsBuilder departAimedActual(String aimedTime, String actualTime) {
      var call = calls.getLast();
      call.setAimedDepartureTime(localTimeParser.zonedDateTime(aimedTime));
      call.setActualDepartureTime(localTimeParser.zonedDateTime(actualTime));
      return this;
    }

    public RecordedCallsBuilder departAimedExpected(String aimedTime, String expectedTime) {
      var call = calls.getLast();
      call.setAimedDepartureTime(localTimeParser.zonedDateTime(aimedTime));
      call.setExpectedDepartureTime(localTimeParser.zonedDateTime(expectedTime));
      return this;
    }

    public RecordedCallsBuilder withVisitNumber(int visitNumber) {
      var call = calls.getLast();
      call.setVisitNumber(BigInteger.valueOf(visitNumber));
      return this;
    }

    public RecordedCallsBuilder withIsExtraCall(boolean extra) {
      var call = calls.getLast();
      call.setExtraCall(extra);
      return this;
    }

    public RecordedCallsBuilder withIsCancellation(boolean cancel) {
      var call = calls.getLast();
      call.setCancellation(cancel);
      return this;
    }

    public RecordedCallsBuilder clearOrder() {
      var call = calls.getLast();
      call.setOrder(null);
      return this;
    }

    public RecordedCallsBuilder addDestinationDisplay(String destinationDisplay) {
      var dd = new NaturalLanguageStringStructure();
      dd.setValue(destinationDisplay);
      calls.getLast().getDestinationDisplaies().add(dd);
      return this;
    }

    public List<RecordedCall> build() {
      return calls;
    }
  }

  public static class EstimatedCallsBuilder {

    private final ArrayList<EstimatedCall> calls;
    private final int orderOffset;
    private final LocalTimeParser localTimeParser;

    public EstimatedCallsBuilder(LocalTimeParser localTimeParser, int orderOffset) {
      this.localTimeParser = localTimeParser;
      this.orderOffset = orderOffset;
      this.calls = new ArrayList<>();
    }

    public EstimatedCallsBuilder call(StopLocation stop) {
      return call(stop.getId().getId());
    }

    public EstimatedCallsBuilder call(String stopPointRef) {
      var call = new EstimatedCall();
      call.setOrder(BigInteger.valueOf(orderOffset + calls.size()));

      var ref = new StopPointRefStructure();
      ref.setValue(stopPointRef);
      call.setStopPointRef(ref);

      calls.add(call);
      return this;
    }

    public EstimatedCallsBuilder arriveAimedExpected(
      @Nullable String aimedTime,
      @Nullable String expectedTime
    ) {
      var call = calls.getLast();
      if (aimedTime != null) {
        call.setAimedArrivalTime(localTimeParser.zonedDateTime(aimedTime));
      }
      if (expectedTime != null) {
        call.setExpectedArrivalTime(localTimeParser.zonedDateTime(expectedTime));
      }
      return this;
    }

    public EstimatedCallsBuilder departAimedExpected(
      @Nullable String aimedTime,
      @Nullable String expectedTime
    ) {
      var call = calls.getLast();
      if (aimedTime != null) {
        call.setAimedDepartureTime(localTimeParser.zonedDateTime(aimedTime));
      }
      if (expectedTime != null) {
        call.setExpectedDepartureTime(localTimeParser.zonedDateTime(expectedTime));
      }
      return this;
    }

    public EstimatedCallsBuilder withVisitNumber(int visitNumber) {
      var call = calls.getLast();
      call.setVisitNumber(BigInteger.valueOf(visitNumber));
      return this;
    }

    public EstimatedCallsBuilder withIsExtraCall(boolean extra) {
      var call = calls.getLast();
      call.setExtraCall(extra);
      return this;
    }

    public EstimatedCallsBuilder withIsCancellation(boolean cancel) {
      var call = calls.getLast();
      call.setCancellation(cancel);
      return this;
    }

    public EstimatedCallsBuilder withArrivalStopAssignment(
      RegularStop aimedQuay,
      @Nullable RegularStop expectedQuay
    ) {
      var stopAssignmentStructure = new StopAssignmentStructure();

      var aimed = new QuayRefStructure();
      aimed.setValue(aimedQuay.getId().getId());
      stopAssignmentStructure.setAimedQuayRef(aimed);

      if (expectedQuay != null) {
        var expected = new QuayRefStructure();
        expected.setValue(expectedQuay.getId().getId());
        stopAssignmentStructure.setExpectedQuayRef(expected);
      }

      var call = calls.getLast();
      call.getArrivalStopAssignments().add(stopAssignmentStructure);
      return this;
    }

    public EstimatedCallsBuilder clearOrder() {
      var call = calls.getLast();
      call.setOrder(null);
      return this;
    }

    public EstimatedCallsBuilder addDestinationDisplay(String destinationDisplay) {
      var dd = new NaturalLanguageStringStructure();
      dd.setValue(destinationDisplay);
      calls.getLast().getDestinationDisplaies().add(dd);
      return this;
    }

    public EstimatedCallsBuilder withArrivalStatus(CallStatusEnumeration callStatus) {
      calls.getLast().setArrivalStatus(callStatus);
      return this;
    }

    public EstimatedCallsBuilder withAimedArrivalTime(String aimedTime) {
      var call = calls.getLast();
      call.setAimedArrivalTime(localTimeParser.zonedDateTime(aimedTime));
      return this;
    }

    public EstimatedCallsBuilder withExpectedArrivalTime(String expectedTime) {
      var call = calls.getLast();
      call.setExpectedArrivalTime(localTimeParser.zonedDateTime(expectedTime));
      return this;
    }

    public EstimatedCallsBuilder withAimedDepartureTime(String aimedTime) {
      var call = calls.getLast();
      call.setAimedDepartureTime(localTimeParser.zonedDateTime(aimedTime));
      return this;
    }

    public EstimatedCallsBuilder withExpectedDepartureTime(String expectedTime) {
      var call = calls.getLast();
      call.setExpectedDepartureTime(localTimeParser.zonedDateTime(expectedTime));
      return this;
    }

    public EstimatedCallsBuilder withCancellation(boolean cancel) {
      var call = calls.getLast();
      call.setCancellation(cancel);
      return this;
    }

    public EstimatedCallsBuilder withPredictionInaccurate(boolean inaccurate) {
      var call = calls.getLast();
      call.setPredictionInaccurate(inaccurate);
      return this;
    }

    public EstimatedCallsBuilder withArrivalBoardingActivity(
      ArrivalBoardingActivityEnumeration activity
    ) {
      calls.getLast().setArrivalBoardingActivity(activity);
      return this;
    }

    public EstimatedCallsBuilder withDepartureBoardingActivity(
      DepartureBoardingActivityEnumeration activity
    ) {
      calls.getLast().setDepartureBoardingActivity(activity);
      return this;
    }

    public EstimatedCallsBuilder withDestinationDisplay(String destinationDisplay) {
      var dd = new NaturalLanguageStringStructure();
      dd.setValue(destinationDisplay);
      calls.getLast().getDestinationDisplaies().add(dd);
      return this;
    }

    public EstimatedCallsBuilder withOccupancy(uk.org.siri.siri21.OccupancyEnumeration occupancy) {
      var call = calls.getLast();
      call.setOccupancy(occupancy);
      return this;
    }

    public EstimatedCallsBuilder next() {
      return this;
    }

    public List<EstimatedCall> build() {
      return calls;
    }
  }
}
