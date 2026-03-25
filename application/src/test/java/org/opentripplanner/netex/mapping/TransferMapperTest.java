package org.opentripplanner.netex.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.opentripplanner.transfer.constrained.internal.DefaultConstrainedTransferService;
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

    var transfers = mapper.mapToTransfers(interchange);
    assertEquals(1, transfers.size());
    var transfer = transfers.getFirst();

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

    var transfer = mapper.mapToTransfers(interchange).getFirst();

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

    var transfer = mapper.mapToTransfers(interchange).getFirst();

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

    var transfers = mapper.mapToTransfers(interchange);

    assertFalse(transfers.isEmpty());
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

    var transfers = mapper.mapToTransfers(interchange);

    assertFalse(transfers.isEmpty());
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

    var transfers = mapper.mapToTransfers(interchange);

    assertFalse(transfers.isEmpty());
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

    var transfers = mapper.mapToTransfers(interchange);

    assertFalse(transfers.isEmpty());
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

    assertTrue(mapper.mapToTransfers(interchange).isEmpty());
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

    assertTrue(mapper.mapToTransfers(interchange).isEmpty());
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

    assertTrue(mapper.mapToTransfers(interchange).isEmpty());
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

    assertTrue(mapper.mapToTransfers(interchange).isEmpty());
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

    assertTrue(mapper.mapToTransfers(interchange).isEmpty());
    assertThat(listTypes(issueStore)).contains("InterchangeWithoutConstraint");
  }

  @Test
  void createsTransferForEachOccurrenceOfFromStop() {
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

    var transfers = mapper.mapToTransfers(interchange);

    assertEquals(2, transfers.size());
    assertEquals(0, ((TripTransferPoint) transfers.get(0).getFrom()).getStopPositionInPattern());
    assertEquals(2, ((TripTransferPoint) transfers.get(1).getFrom()).getStopPositionInPattern());
  }

  /**
   * Verifies that a guaranteed interchange is found at all stop positions when the feeder trip
   * visits the transfer stop twice (loop pattern). See
   * <a href="https://github.com/opentripplanner/OpenTripPlanner/issues/7466">#7466</a>.
   */
  @Test
  void interchangeFoundAtAllPositionsWhenFeederTripVisitsTransferStopTwice() {
    var transferStopId = "TEST:ScheduledStopPoint:TransferStop";
    int firstVisitPosition = 2;
    int secondVisitPosition = 5;
    var stopPointsIndex = Map.of(
      FROM_JOURNEY_ID,
      List.of(
        "TEST:ScheduledStopPoint:Start",
        "TEST:ScheduledStopPoint:StopA",
        // Position 2: first visit
        transferStopId,
        "TEST:ScheduledStopPoint:StopB",
        "TEST:ScheduledStopPoint:StopC",
        // Position 5: second visit (loop returns through same stop)
        transferStopId,
        "TEST:ScheduledStopPoint:End"
      ),
      TO_JOURNEY_ID,
      List.of(transferStopId, "TEST:ScheduledStopPoint:Destination")
    );

    var trips = createTripsIndex();
    var mapper = new TransferMapper(ID_FACTORY, DataImportIssueStore.NOOP, stopPointsIndex, trips);

    var interchange = new ServiceJourneyInterchange()
      .withId(INTERCHANGE_ID)
      .withFromJourneyRef(createJourneyRef(FROM_JOURNEY_ID))
      .withToJourneyRef(createJourneyRef(TO_JOURNEY_ID))
      .withFromPointRef(createStopRef(transferStopId))
      .withToPointRef(createStopRef(transferStopId))
      .withGuaranteed(true)
      .withMaximumWaitTime(Duration.ofMinutes(5));

    var transfers = mapper.mapToTransfers(interchange);
    assertEquals(2, transfers.size(), "One transfer per from-stop occurrence");
    assertTrue(transfers.getFirst().getTransferConstraint().isGuaranteed());

    // Store the transfers in the ConstrainedTransferService (as the graph builder does)
    var transferService = new DefaultConstrainedTransferService();
    transferService.addAll(transfers);

    var fromTrip = trips.get(ID_FACTORY.createId(FROM_JOURNEY_ID));
    var toTrip = trips.get(ID_FACTORY.createId(TO_JOURNEY_ID));
    var testModel = TimetableRepositoryForTest.of();
    var stop = testModel.stop("TransferStop", 59.0, 6.0).build();

    // Lookup succeeds at BOTH stop positions
    assertNotNull(
      transferService.findTransfer(fromTrip, firstVisitPosition, stop, toTrip, 0, stop),
      "Transfer found at first visit position"
    );
    assertNotNull(
      transferService.findTransfer(fromTrip, secondVisitPosition, stop, toTrip, 0, stop),
      "Transfer found at second visit position"
    );
  }

  @Test
  void createsTransferForEachOccurrenceOfToStop() {
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

    var transfers = mapper.mapToTransfers(interchange);

    assertEquals(2, transfers.size());
    assertEquals(0, ((TripTransferPoint) transfers.get(0).getTo()).getStopPositionInPattern());
    assertEquals(2, ((TripTransferPoint) transfers.get(1).getTo()).getStopPositionInPattern());
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
