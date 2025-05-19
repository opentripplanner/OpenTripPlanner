package org.opentripplanner.updater.trip.siri;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.opentripplanner.DateTimeHelper;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import uk.org.siri.siri21.DataFrameRefStructure;
import uk.org.siri.siri21.DatedVehicleJourneyRef;
import uk.org.siri.siri21.EstimatedCall;
import uk.org.siri.siri21.EstimatedTimetableDeliveryStructure;
import uk.org.siri.siri21.EstimatedVehicleJourney;
import uk.org.siri.siri21.EstimatedVersionFrameStructure;
import uk.org.siri.siri21.FramedVehicleJourneyRefStructure;
import uk.org.siri.siri21.LineRef;
import uk.org.siri.siri21.OperatorRefStructure;
import uk.org.siri.siri21.QuayRefStructure;
import uk.org.siri.siri21.RecordedCall;
import uk.org.siri.siri21.StopAssignmentStructure;
import uk.org.siri.siri21.StopPointRefStructure;
import uk.org.siri.siri21.VehicleJourneyRef;

/**
 * This is a helper class for constucting Siri ET messages to use in tests.
 */
public class SiriEtBuilder {

  private final EstimatedVehicleJourney evj;
  private final DateTimeHelper dateTimeHelper;

  public SiriEtBuilder(DateTimeHelper dateTimeHelper) {
    this.dateTimeHelper = dateTimeHelper;
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

  public SiriEtBuilder withRecordedCalls(
    Function<RecordedCallsBuilder, RecordedCallsBuilder> producer
  ) {
    if (evj.getEstimatedCalls() != null) {
      // If we call this after estimatedCalls, the ordering will be messed up
      throw new RuntimeException(
        "You need to call withRecordedCalls() before withEstimatedCalls()"
      );
    }
    var builder = new RecordedCallsBuilder(dateTimeHelper, 0);

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
    var builder = new EstimatedCallsBuilder(dateTimeHelper, offset);

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
    private final DateTimeHelper dateTimeHelper;

    public RecordedCallsBuilder(DateTimeHelper dateTimeHelper, int orderOffset) {
      this.dateTimeHelper = dateTimeHelper;
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
      call.setAimedArrivalTime(dateTimeHelper.zonedDateTime(aimedTime));
      call.setActualArrivalTime(dateTimeHelper.zonedDateTime(actualTime));
      return this;
    }

    public RecordedCallsBuilder departAimedActual(String aimedTime, String actualTime) {
      var call = calls.getLast();
      call.setAimedDepartureTime(dateTimeHelper.zonedDateTime(aimedTime));
      call.setActualDepartureTime(dateTimeHelper.zonedDateTime(actualTime));
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

    public List<RecordedCall> build() {
      return calls;
    }
  }

  public static class EstimatedCallsBuilder {

    private final ArrayList<EstimatedCall> calls;
    private final int orderOffset;
    private final DateTimeHelper dateTimeHelper;

    public EstimatedCallsBuilder(DateTimeHelper dateTimeHelper, int orderOffset) {
      this.dateTimeHelper = dateTimeHelper;
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
        call.setAimedArrivalTime(dateTimeHelper.zonedDateTime(aimedTime));
      }
      if (expectedTime != null) {
        call.setExpectedArrivalTime(dateTimeHelper.zonedDateTime(expectedTime));
      }
      return this;
    }

    public EstimatedCallsBuilder departAimedExpected(
      @Nullable String aimedTime,
      @Nullable String expectedTime
    ) {
      var call = calls.getLast();
      if (aimedTime != null) {
        call.setAimedDepartureTime(dateTimeHelper.zonedDateTime(aimedTime));
      }
      if (expectedTime != null) {
        call.setExpectedDepartureTime(dateTimeHelper.zonedDateTime(expectedTime));
      }
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

    public List<EstimatedCall> build() {
      return calls;
    }
  }
}
