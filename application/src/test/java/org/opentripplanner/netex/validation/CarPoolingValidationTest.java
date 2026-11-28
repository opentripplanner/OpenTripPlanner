package org.opentripplanner.netex.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.netex.NetexTestDataSupport.createCarPoolingService;
import static org.opentripplanner.netex.NetexTestDataSupport.createOperatingDay;
import static org.opentripplanner.netex.NetexTestDataSupport.createSingleJourney;
import static org.opentripplanner.netex.NetexTestDataSupport.createSingleJourneyPath;

import java.time.LocalDate;
import java.time.LocalTime;
import org.entur.netex2.model.Authority;
import org.entur.netex2.model.CarPoolingService;
import org.entur.netex2.model.Operator;
import org.entur.netex2.model.Quay;
import org.entur.netex2.model.SingleJourney;
import org.entur.netex2.model.SingleJourneyPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.netex.index.NetexEntityIndex;

class CarPoolingValidationTest {

  private static final String OPERATOR_ID = "RID:Operator:1";
  private static final String SERVICE_ID = "RID:CarPoolingService:1";
  private static final String OTHER_SERVICE_ID = "RID:CarPoolingService:2";
  private static final String OPERATING_DAY_ID = "RID:OperatingDay:1";
  private static final String PATH_ID = "RID:SingleJourneyPath:1";
  private static final String JOURNEY_ID = "RID:SingleJourney:1";
  private static final String QUAY_ID = "NSR:Quay:1";
  private static final String UNKNOWN_ID = "RID:Unknown:1";
  private static final LocalTime T08_00 = LocalTime.of(8, 0);
  private static final LocalTime T08_15 = LocalTime.of(8, 15);

  private final NetexEntityIndex index = new NetexEntityIndex();
  private final SingleJourneyPath path = createSingleJourneyPath(PATH_ID, "A", "B");
  private final CarPoolingService service = createCarPoolingService(
    SERVICE_ID,
    "Ride Along",
    OPERATOR_ID
  );

  @BeforeEach
  void setUp() {
    index.operatorsById.add(new Operator().withId(OPERATOR_ID));
    index.carPoolingServiceById.add(service);
    index.operatingDayById.add(createOperatingDay(OPERATING_DAY_ID, LocalDate.of(2026, 12, 1)));
    index.singleJourneyPathById.add(path);
  }

  @Test
  void validJourneyIsKept() {
    add(validJourney());

    assertTrue(journeyIsKept());
  }

  @Test
  void journeyOfUnknownCarPoolingServiceIsDiscarded() {
    add(createSingleJourney(JOURNEY_ID, UNKNOWN_ID, OPERATING_DAY_ID, path, T08_00, T08_15));

    assertFalse(journeyIsKept());
  }

  @Test
  void journeyWithoutServiceIsDiscarded() {
    add(validJourney().withCommonVehicleServiceRef(null));

    assertFalse(journeyIsKept());
  }

  @Test
  void journeyOnUnknownOperatingDayIsDiscarded() {
    add(createSingleJourney(JOURNEY_ID, SERVICE_ID, UNKNOWN_ID, path, T08_00, T08_15));

    assertFalse(journeyIsKept());
  }

  @Test
  void journeyWithoutOperatingDayIsDiscarded() {
    add(validJourney().withOperatingDayRef(null));

    assertFalse(journeyIsKept());
  }

  @Test
  void journeyOnUnknownPathIsDiscarded() {
    var unknownPath = createSingleJourneyPath(UNKNOWN_ID, "A", "B");
    add(createSingleJourney(JOURNEY_ID, SERVICE_ID, OPERATING_DAY_ID, unknownPath, T08_00, T08_15));

    assertFalse(journeyIsKept());
  }

  @Test
  void journeyWithoutPathIsDiscarded() {
    add(validJourney().withSingleJourneyPathRef(null));

    assertFalse(journeyIsKept());
  }

  @Test
  void journeyInAnotherFileThanItsPathIsDiscarded() {
    var otherFile = index.push();
    otherFile.singleJourneyById.add(validJourney());

    Validator.validate(otherFile, DataImportIssueStore.NOOP);

    assertFalse(otherFile.singleJourneyById.containsKey(JOURNEY_ID));
  }

  @Test
  void journeyWithoutPassingTimesIsDiscarded() {
    add(validJourney().withDatedPassingTimes(null));

    assertFalse(journeyIsKept());
  }

  @Test
  void journeyWithoutPassingTimeForEachPointIsDiscarded() {
    add(createSingleJourney(JOURNEY_ID, SERVICE_ID, OPERATING_DAY_ID, path, T08_00));

    assertFalse(journeyIsKept());
  }

  @Test
  void journeyWithPassingTimesInAnotherOrderThanThePathIsDiscarded() {
    var reversedPath = createSingleJourneyPath(PATH_ID, "B", "A");
    add(
      createSingleJourney(JOURNEY_ID, SERVICE_ID, OPERATING_DAY_ID, reversedPath, T08_00, T08_15)
    );

    assertFalse(journeyIsKept());
  }

  @Test
  void journeyWithPassingTimeWithoutTimeIsDiscarded() {
    add(createSingleJourney(JOURNEY_ID, SERVICE_ID, OPERATING_DAY_ID, path, T08_00, null));

    assertFalse(journeyIsKept());
  }

  @Test
  void journeyGoingBackInTimeIsDiscarded() {
    add(createSingleJourney(JOURNEY_ID, SERVICE_ID, OPERATING_DAY_ID, path, T08_15, T08_00));

    assertFalse(journeyIsKept());
  }

  @Test
  void journeyOnPathWithOnePointIsDiscarded() {
    var shortPath = createSingleJourneyPath(PATH_ID, "A");
    index.singleJourneyPathById.add(shortPath);
    add(createSingleJourney(JOURNEY_ID, SERVICE_ID, OPERATING_DAY_ID, shortPath, T08_00));

    assertFalse(journeyIsKept());
  }

  @Test
  void journeyOnPathWithoutPointsIsDiscarded() {
    add(validJourney());
    path.setPointsInSequence(null);

    assertFalse(journeyIsKept());
  }

  @Test
  void pathHasTheJourneysOfOneServiceOnly() {
    index.carPoolingServiceById.add(
      createCarPoolingService(OTHER_SERVICE_ID, "Other", OPERATOR_ID)
    );
    add(validJourney());
    add(
      createSingleJourney(
        "RID:SingleJourney:2",
        OTHER_SERVICE_ID,
        OPERATING_DAY_ID,
        path,
        T08_00,
        T08_15
      )
    );

    Validator.validate(index, DataImportIssueStore.NOOP);

    assertEquals(1, index.singleJourneyById.localKeys().size());
  }

  @Test
  void serviceOfAuthorityIsKept() {
    index.authoritiesById.add(new Authority().withId("RID:Authority:1"));
    index.carPoolingServiceById.add(
      createCarPoolingService(SERVICE_ID, "Ride Along", "RID:Authority:1")
    );

    Validator.validate(index, DataImportIssueStore.NOOP);

    assertTrue(index.carPoolingServiceById.containsKey(SERVICE_ID));
  }

  @Test
  void serviceOfUnknownOrganisationIsDiscardedWithItsJourneys() {
    index.carPoolingServiceById.add(createCarPoolingService(SERVICE_ID, "Ride Along", UNKNOWN_ID));
    add(validJourney());

    assertFalse(journeyIsKept());
    assertFalse(index.carPoolingServiceById.containsKey(SERVICE_ID));
  }

  @Test
  void serviceWithoutOrganisationIsDiscarded() {
    service.setOrganisationRef(null);

    Validator.validate(index, DataImportIssueStore.NOOP);

    assertFalse(index.carPoolingServiceById.containsKey(SERVICE_ID));
  }

  @Test
  void assignmentOfMeetingPointToUnknownQuayIsDiscarded() {
    index.quayById.add(new Quay().withId(QUAY_ID));
    index.quayIdByVehicleMeetingPointRef.add("A", QUAY_ID);
    index.quayIdByVehicleMeetingPointRef.add("B", UNKNOWN_ID);

    Validator.validate(index, DataImportIssueStore.NOOP);

    assertEquals(QUAY_ID, index.quayIdByVehicleMeetingPointRef.lookup("A"));
    assertNull(index.quayIdByVehicleMeetingPointRef.lookup("B"));
  }

  private SingleJourney validJourney() {
    return createSingleJourney(JOURNEY_ID, SERVICE_ID, OPERATING_DAY_ID, path, T08_00, T08_15);
  }

  private void add(SingleJourney journey) {
    index.singleJourneyById.add(journey);
  }

  private boolean journeyIsKept() {
    Validator.validate(index, DataImportIssueStore.NOOP);
    return index.singleJourneyById.containsKey(JOURNEY_ID);
  }
}
