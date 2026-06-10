package org.opentripplanner.netex.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.netex.mapping.MappingSupport.ID_FACTORY;

import java.math.BigInteger;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.transfer.constrained.model.TripTransferPoint;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.DefaultEntityById;
import org.opentripplanner.transit.model.timetable.Trip;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
import org.rutebanken.netex.model.ServiceJourneyInterchange;
import org.rutebanken.netex.model.VehicleJourneyRefStructure;

class TransferMapperTest {

  private static final String INTERCHANGE_ID = "TEST:ServiceJourneyInterchange:1";
  private static final String FROM_JOURNEY_ID = "TEST:ServiceJourney:1";
  private static final String TO_JOURNEY_ID = "TEST:ServiceJourney:2";
  private static final String FROM_STOP_ID = "TEST:ScheduledStopPoint:1";
  private static final String TO_STOP_ID = "TEST:ScheduledStopPoint:2";

  @Test
  void mapGuaranteedTransfer() {
    var trips = createTripsIndex();
    var stopPointsIndex = createStopPointsIndex();

    var mapper = new TransferMapper(ID_FACTORY, DataImportIssueStore.NOOP, stopPointsIndex, trips);

    var interchange = new ServiceJourneyInterchange()
      .withId(INTERCHANGE_ID)
      .withFromJourneyRef(createJourneyRef(FROM_JOURNEY_ID))
      .withToJourneyRef(createJourneyRef(TO_JOURNEY_ID))
      .withFromPointRef(createStopRef(FROM_STOP_ID))
      .withToPointRef(createStopRef(TO_STOP_ID))
      .withGuaranteed(true)
      .withPriority(BigInteger.valueOf(2));

    var transfer = mapper.mapToTransfer(interchange);

    assertEquals(ID_FACTORY.createId(INTERCHANGE_ID), transfer.getId());
    assertTrue(transfer.getTransferConstraint().isGuaranteed());
    assertFalse(transfer.getTransferConstraint().isStaySeated());
  }

  @Test
  void mapStaySeatedTransfer() {
    var trips = createTripsIndex();
    var stopPointsIndex = createStopPointsIndex();

    var mapper = new TransferMapper(ID_FACTORY, DataImportIssueStore.NOOP, stopPointsIndex, trips);

    var interchange = new ServiceJourneyInterchange()
      .withId(INTERCHANGE_ID)
      .withFromJourneyRef(createJourneyRef(FROM_JOURNEY_ID))
      .withToJourneyRef(createJourneyRef(TO_JOURNEY_ID))
      .withFromPointRef(createStopRef(FROM_STOP_ID))
      .withToPointRef(createStopRef(TO_STOP_ID))
      .withStaySeated(true);

    var transfer = mapper.mapToTransfer(interchange);

    assertTrue(transfer.getTransferConstraint().isStaySeated());
  }

  @Test
  void mapTransferWithMaxWaitTime() {
    var trips = createTripsIndex();
    var stopPointsIndex = createStopPointsIndex();

    var mapper = new TransferMapper(ID_FACTORY, DataImportIssueStore.NOOP, stopPointsIndex, trips);

    var interchange = new ServiceJourneyInterchange()
      .withId(INTERCHANGE_ID)
      .withFromJourneyRef(createJourneyRef(FROM_JOURNEY_ID))
      .withToJourneyRef(createJourneyRef(TO_JOURNEY_ID))
      .withFromPointRef(createStopRef(FROM_STOP_ID))
      .withToPointRef(createStopRef(TO_STOP_ID))
      .withGuaranteed(true)
      .withMaximumWaitTime(Duration.ofMinutes(5));

    var transfer = mapper.mapToTransfer(interchange);

    assertEquals(300, transfer.getTransferConstraint().getMaxWaitTime());
  }

  @Test
  void mapTransferWithPriorityNotAllowed() {
    var trips = createTripsIndex();
    var stopPointsIndex = createStopPointsIndex();

    var mapper = new TransferMapper(ID_FACTORY, DataImportIssueStore.NOOP, stopPointsIndex, trips);

    var interchange = new ServiceJourneyInterchange()
      .withId(INTERCHANGE_ID)
      .withFromJourneyRef(createJourneyRef(FROM_JOURNEY_ID))
      .withToJourneyRef(createJourneyRef(TO_JOURNEY_ID))
      .withFromPointRef(createStopRef(FROM_STOP_ID))
      .withToPointRef(createStopRef(TO_STOP_ID))
      .withPriority(BigInteger.valueOf(-1));

    var transfer = mapper.mapToTransfer(interchange);

    assertNotNull(transfer);
  }

  @Test
  void mapTransferWithPriorityAllowed() {
    var trips = createTripsIndex();
    var stopPointsIndex = createStopPointsIndex();

    var mapper = new TransferMapper(ID_FACTORY, DataImportIssueStore.NOOP, stopPointsIndex, trips);

    var interchange = new ServiceJourneyInterchange()
      .withId(INTERCHANGE_ID)
      .withFromJourneyRef(createJourneyRef(FROM_JOURNEY_ID))
      .withToJourneyRef(createJourneyRef(TO_JOURNEY_ID))
      .withFromPointRef(createStopRef(FROM_STOP_ID))
      .withToPointRef(createStopRef(TO_STOP_ID))
      .withPriority(BigInteger.valueOf(0))
      .withGuaranteed(true);

    var transfer = mapper.mapToTransfer(interchange);

    assertNotNull(transfer);
  }

  @Test
  void mapTransferWithPriorityRecommended() {
    var trips = createTripsIndex();
    var stopPointsIndex = createStopPointsIndex();

    var mapper = new TransferMapper(ID_FACTORY, DataImportIssueStore.NOOP, stopPointsIndex, trips);

    var interchange = new ServiceJourneyInterchange()
      .withId(INTERCHANGE_ID)
      .withFromJourneyRef(createJourneyRef(FROM_JOURNEY_ID))
      .withToJourneyRef(createJourneyRef(TO_JOURNEY_ID))
      .withFromPointRef(createStopRef(FROM_STOP_ID))
      .withToPointRef(createStopRef(TO_STOP_ID))
      .withPriority(BigInteger.valueOf(1));

    var transfer = mapper.mapToTransfer(interchange);

    assertNotNull(transfer);
  }

  @Test
  void mapTransferWithPriorityPreferred() {
    var trips = createTripsIndex();
    var stopPointsIndex = createStopPointsIndex();

    var mapper = new TransferMapper(ID_FACTORY, DataImportIssueStore.NOOP, stopPointsIndex, trips);

    var interchange = new ServiceJourneyInterchange()
      .withId(INTERCHANGE_ID)
      .withFromJourneyRef(createJourneyRef(FROM_JOURNEY_ID))
      .withToJourneyRef(createJourneyRef(TO_JOURNEY_ID))
      .withFromPointRef(createStopRef(FROM_STOP_ID))
      .withToPointRef(createStopRef(TO_STOP_ID))
      .withPriority(BigInteger.valueOf(2));

    var transfer = mapper.mapToTransfer(interchange);

    assertNotNull(transfer);
  }

  @Test
  void returnsNullWhenFromJourneyRefIsNull() {
    var trips = createTripsIndex();
    var stopPointsIndex = createStopPointsIndex();
    var issueStore = new DefaultDataImportIssueStore();

    var mapper = new TransferMapper(ID_FACTORY, issueStore, stopPointsIndex, trips);

    var interchange = new ServiceJourneyInterchange()
      .withId(INTERCHANGE_ID)
      .withToJourneyRef(createJourneyRef(TO_JOURNEY_ID))
      .withFromPointRef(createStopRef(FROM_STOP_ID))
      .withToPointRef(createStopRef(TO_STOP_ID))
      .withGuaranteed(true);

    assertNull(mapper.mapToTransfer(interchange));
    assertThat(listTypes(issueStore)).contains("InvalidInterchange");
  }

  @Test
  void returnsNullWhenToJourneyRefIsNull() {
    var trips = createTripsIndex();
    var stopPointsIndex = createStopPointsIndex();
    var issueStore = new DefaultDataImportIssueStore();

    var mapper = new TransferMapper(ID_FACTORY, issueStore, stopPointsIndex, trips);

    var interchange = new ServiceJourneyInterchange()
      .withId(INTERCHANGE_ID)
      .withFromJourneyRef(createJourneyRef(FROM_JOURNEY_ID))
      .withFromPointRef(createStopRef(FROM_STOP_ID))
      .withToPointRef(createStopRef(TO_STOP_ID))
      .withGuaranteed(true);

    assertNull(mapper.mapToTransfer(interchange));
    assertThat(listTypes(issueStore)).contains("InvalidInterchange");
  }

  @Test
  void returnsNullWhenTripNotFound() {
    var trips = new DefaultEntityById<Trip>();
    var stopPointsIndex = createStopPointsIndex();

    var issueStore = new DefaultDataImportIssueStore();
    var mapper = new TransferMapper(ID_FACTORY, issueStore, stopPointsIndex, trips);

    var interchange = new ServiceJourneyInterchange()
      .withId(INTERCHANGE_ID)
      .withFromJourneyRef(createJourneyRef(FROM_JOURNEY_ID))
      .withToJourneyRef(createJourneyRef(TO_JOURNEY_ID))
      .withFromPointRef(createStopRef(FROM_STOP_ID))
      .withToPointRef(createStopRef(TO_STOP_ID))
      .withGuaranteed(true);

    assertNull(mapper.mapToTransfer(interchange));
    assertThat(listTypes(issueStore)).contains("InvalidInterchange");
  }

  @Test
  void returnsNullWhenStopPointNotFoundInIndex() {
    var trips = createTripsIndex();
    var stopPointsIndex = Map.<String, List<String>>of();

    var issueStore = new DefaultDataImportIssueStore();
    var mapper = new TransferMapper(ID_FACTORY, issueStore, stopPointsIndex, trips);

    var interchange = new ServiceJourneyInterchange()
      .withId(INTERCHANGE_ID)
      .withFromJourneyRef(createJourneyRef(FROM_JOURNEY_ID))
      .withToJourneyRef(createJourneyRef(TO_JOURNEY_ID))
      .withFromPointRef(createStopRef(FROM_STOP_ID))
      .withToPointRef(createStopRef(TO_STOP_ID))
      .withGuaranteed(true);

    assertNull(mapper.mapToTransfer(interchange));
    assertThat(listTypes(issueStore)).contains("InvalidInterchange");
  }

  @Test
  void returnsNullWhenInterchangeHasNoConstraints() {
    var trips = createTripsIndex();
    var stopPointsIndex = createStopPointsIndex();

    var issueStore = new DefaultDataImportIssueStore();
    var mapper = new TransferMapper(ID_FACTORY, issueStore, stopPointsIndex, trips);

    var interchange = new ServiceJourneyInterchange()
      .withId(INTERCHANGE_ID)
      .withFromJourneyRef(createJourneyRef(FROM_JOURNEY_ID))
      .withToJourneyRef(createJourneyRef(TO_JOURNEY_ID))
      .withFromPointRef(createStopRef(FROM_STOP_ID))
      .withToPointRef(createStopRef(TO_STOP_ID));

    assertNull(mapper.mapToTransfer(interchange));
    assertThat(listTypes(issueStore)).contains("InterchangeWithoutConstraint");
  }

  @Test
  void usesLastIndexForFromStop() {
    var trips = createTripsIndex();
    var stopPointsIndex = Map.of(
      FROM_JOURNEY_ID,
      List.of(FROM_STOP_ID, "OTHER_STOP", FROM_STOP_ID),
      TO_JOURNEY_ID,
      List.of(TO_STOP_ID)
    );

    var mapper = new TransferMapper(ID_FACTORY, DataImportIssueStore.NOOP, stopPointsIndex, trips);

    var interchange = new ServiceJourneyInterchange()
      .withId(INTERCHANGE_ID)
      .withFromJourneyRef(createJourneyRef(FROM_JOURNEY_ID))
      .withToJourneyRef(createJourneyRef(TO_JOURNEY_ID))
      .withFromPointRef(createStopRef(FROM_STOP_ID))
      .withToPointRef(createStopRef(TO_STOP_ID))
      .withGuaranteed(true);

    var transfer = mapper.mapToTransfer(interchange);

    assertEquals(2, ((TripTransferPoint) transfer.getFrom()).getStopPositionInPattern());
  }

  /**
   * When two visits to the same Quay use distinct ScheduledStopPoint IDs, the interchange
   * reference is unambiguous — the SSP appears only once in the stop list, so both indexOf and
   * lastIndexOf return the correct position. This is the ideal data modeling for loop patterns.
   */
  @Test
  void distinctSspPerVisitDisambiguatesLoopPattern() {
    var transferStopVisit1 = "TEST:ScheduledStopPoint:TransferStop_visit1";
    var transferStopVisit2 = "TEST:ScheduledStopPoint:TransferStop_visit2";
    var stopPointsIndex = Map.of(
      FROM_JOURNEY_ID,
      List.of(
        "TEST:ScheduledStopPoint:Start",
        "TEST:ScheduledStopPoint:StopA",
        // Position 2: first visit (same Quay)
        transferStopVisit1,
        "TEST:ScheduledStopPoint:StopB",
        "TEST:ScheduledStopPoint:StopC",
        // Position 5: second visit (same Quay, different SSP)
        transferStopVisit2,
        "TEST:ScheduledStopPoint:End"
      ),
      TO_JOURNEY_ID,
      List.of(transferStopVisit1, "TEST:ScheduledStopPoint:Destination")
    );

    var trips = createTripsIndex();
    var mapper = new TransferMapper(ID_FACTORY, DataImportIssueStore.NOOP, stopPointsIndex, trips);

    // Interchange references the first-visit SSP
    var interchange = new ServiceJourneyInterchange()
      .withId(INTERCHANGE_ID)
      .withFromJourneyRef(createJourneyRef(FROM_JOURNEY_ID))
      .withToJourneyRef(createJourneyRef(TO_JOURNEY_ID))
      .withFromPointRef(createStopRef(transferStopVisit1))
      .withToPointRef(createStopRef(transferStopVisit1))
      .withGuaranteed(true)
      .withMaximumWaitTime(Duration.ofMinutes(5));

    var transfer = mapper.mapToTransfer(interchange);

    assertNotNull(transfer);
    assertTrue(transfer.getTransferConstraint().isGuaranteed());
    // The SSP is unique, so the position resolves correctly to the first visit
    assertEquals(2, ((TripTransferPoint) transfer.getFrom()).getStopPositionInPattern());
    assertEquals(0, ((TripTransferPoint) transfer.getTo()).getStopPositionInPattern());
  }

  @Test
  void usesFirstIndexForToStop() {
    var trips = createTripsIndex();
    var stopPointsIndex = Map.of(
      FROM_JOURNEY_ID,
      List.of(FROM_STOP_ID),
      TO_JOURNEY_ID,
      List.of(TO_STOP_ID, "OTHER_STOP", TO_STOP_ID)
    );

    var mapper = new TransferMapper(ID_FACTORY, DataImportIssueStore.NOOP, stopPointsIndex, trips);

    var interchange = new ServiceJourneyInterchange()
      .withId(INTERCHANGE_ID)
      .withFromJourneyRef(createJourneyRef(FROM_JOURNEY_ID))
      .withToJourneyRef(createJourneyRef(TO_JOURNEY_ID))
      .withFromPointRef(createStopRef(FROM_STOP_ID))
      .withToPointRef(createStopRef(TO_STOP_ID))
      .withGuaranteed(true);

    var transfer = mapper.mapToTransfer(interchange);

    assertEquals(0, ((TripTransferPoint) transfer.getTo()).getStopPositionInPattern());
  }

  private DefaultEntityById<Trip> createTripsIndex() {
    var trips = new DefaultEntityById<Trip>();
    trips.add(TimetableRepositoryForTest.trip(FROM_JOURNEY_ID).build());
    trips.add(TimetableRepositoryForTest.trip(TO_JOURNEY_ID).build());
    return trips;
  }

  private Map<String, List<String>> createStopPointsIndex() {
    return Map.of(FROM_JOURNEY_ID, List.of(FROM_STOP_ID), TO_JOURNEY_ID, List.of(TO_STOP_ID));
  }

  private VehicleJourneyRefStructure createJourneyRef(String id) {
    return new VehicleJourneyRefStructure().withRef(id);
  }

  private ScheduledStopPointRefStructure createStopRef(String id) {
    return new ScheduledStopPointRefStructure().withRef(id);
  }

  private static Stream<String> listTypes(DefaultDataImportIssueStore issueStore) {
    return issueStore.listIssues().stream().map(DataImportIssue::getType);
  }
}
