package org.opentripplanner.raptor.rangeraptor.path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.stoparrival.AccessAndEgressWithOpeningHoursPathTestCase.flexCaseAForwardSearch;
import static org.opentripplanner.raptor._data.stoparrival.AccessAndEgressWithOpeningHoursPathTestCase.flexCaseAReverseSearch;
import static org.opentripplanner.raptor._data.stoparrival.AccessAndEgressWithOpeningHoursPathTestCase.flexCaseAText;
import static org.opentripplanner.raptor._data.stoparrival.AccessAndEgressWithOpeningHoursPathTestCase.flexCaseAWithOpeningHoursForwardSearch;
import static org.opentripplanner.raptor._data.stoparrival.AccessAndEgressWithOpeningHoursPathTestCase.flexCaseAWithOpeningHoursReverseSearch;
import static org.opentripplanner.raptor._data.stoparrival.AccessAndEgressWithOpeningHoursPathTestCase.flexCaseAWithOpeningHoursText;
import static org.opentripplanner.raptor._data.stoparrival.AccessAndEgressWithOpeningHoursPathTestCase.flexCaseBForwardSearch;
import static org.opentripplanner.raptor._data.stoparrival.AccessAndEgressWithOpeningHoursPathTestCase.flexCaseBReverseSearch;
import static org.opentripplanner.raptor._data.stoparrival.AccessAndEgressWithOpeningHoursPathTestCase.flexCaseBText;
import static org.opentripplanner.raptor._data.stoparrival.AccessAndEgressWithOpeningHoursPathTestCase.flexCaseBWithOpeningHoursForwardSearch;
import static org.opentripplanner.raptor._data.stoparrival.AccessAndEgressWithOpeningHoursPathTestCase.flexCaseBWithOpeningHoursReverseSearch;
import static org.opentripplanner.raptor._data.stoparrival.AccessAndEgressWithOpeningHoursPathTestCase.flexCaseBWithOpeningHoursText;
import static org.opentripplanner.raptor._data.stoparrival.BasicPathTestCase.BASIC_PATH_AS_DETAILED_STRING;
import static org.opentripplanner.raptor._data.stoparrival.BasicPathTestCase.C1_CALCULATOR;
import static org.opentripplanner.raptor._data.stoparrival.BasicPathTestCase.basicTripByForwardSearch;
import static org.opentripplanner.raptor._data.stoparrival.BasicPathTestCase.basicTripByReverseSearch;
import static org.opentripplanner.raptor._data.stoparrival.BasicPathTestCase.lifeCycle;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.stoparrival.AccessAndEgressWithOpeningHoursPathTestCase;
import org.opentripplanner.raptor._data.transit.TestCostCalculator;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;

public class PathMapperTest implements RaptorTestConstants {

  private static final RaptorSlackProvider FLEX_SLACK_PROVIDER =
    AccessAndEgressWithOpeningHoursPathTestCase.SLACK_PROVIDER;
  private static final TestCostCalculator FLEX_COST_CALCULATOR =
    AccessAndEgressWithOpeningHoursPathTestCase.C1_CALCULATOR;

  /* BASIC CASES */

  @Test
  public void mapToPathBasicForwardSearch() {
    // Given:
    var destArrival = basicTripByForwardSearch();
    var mapper = new ForwardPathMapper<>(
      SLACK_PROVIDER,
      C1_CALCULATOR,
      RaptorTestConstants::stopIndexToName,
      null,
      lifeCycle(),
      false
    );

    //When:
    RaptorPath<TestTripSchedule> path = mapper.mapToPath(destArrival);

    // Then: verify path - should be the same for reverse and forward mapper
    assertPath(path);
  }

  @Test
  public void mapToPathBasicReverseSearch() {
    // Given:
    var destArrival = basicTripByReverseSearch();
    var mapper = new ReversePathMapper<>(
      SLACK_PROVIDER,
      C1_CALCULATOR,
      RaptorTestConstants::stopIndexToName,
      null,
      lifeCycle(),
      false
    );

    //When:
    RaptorPath<TestTripSchedule> path = mapper.mapToPath(destArrival);

    // Then: verify path - should be the same for reverse and forward mapper
    assertPath(path);
  }

  /* FLEX CASES - FORWARD SEARCH */

  @Test
  public void mapToPathForFlexCaseAForwardSearch() {
    runTestFlexForward(flexCaseAForwardSearch(), flexCaseAText());
  }

  @Test
  public void mapToPathForFlexCaseAWOpeningHoursForwardSearch() {
    runTestFlexForward(flexCaseAWithOpeningHoursForwardSearch(), flexCaseAWithOpeningHoursText());
  }

  @Test
  public void mapToPathForFlexCaseBForwardSearch() {
    runTestFlexForward(flexCaseBForwardSearch(), flexCaseBText());
  }

  @Test
  public void mapToPathForFlexCaseBWOpeningHoursForwardSearch() {
    runTestFlexForward(flexCaseBWithOpeningHoursForwardSearch(), flexCaseBWithOpeningHoursText());
  }

  /* FLEX CASES - REVERSE SEARCH */

  @Test
  public void mapToPathForFlexCaseAReverseSearch() {
    runTestFlexReverse(flexCaseAReverseSearch(), flexCaseAText());
  }

  @Test
  public void mapToPathForFlexCaseAWOpeningHoursReverseSearch() {
    runTestFlexReverse(flexCaseAWithOpeningHoursReverseSearch(), flexCaseAWithOpeningHoursText());
  }

  @Test
  public void mapToPathForFlexCaseBReverseSearch() {
    runTestFlexReverse(flexCaseBReverseSearch(), flexCaseBText());
  }

  @Test
  public void mapToPathForFlexCaseBWOpeningHoursReverseSearch() {
    runTestFlexReverse(flexCaseBWithOpeningHoursReverseSearch(), flexCaseBWithOpeningHoursText());
  }

  /* private helper methods */

  private void assertPath(RaptorPath<TestTripSchedule> path) {
    assertEquals(
      BASIC_PATH_AS_DETAILED_STRING,
      path.toStringDetailed(RaptorTestConstants::stopIndexToName)
    );
  }

  private void runTestFlexForward(
    DestinationArrival<TestTripSchedule> destArrival,
    String expected
  ) {
    // Given:
    var mapper = new ForwardPathMapper<TestTripSchedule>(
      FLEX_SLACK_PROVIDER,
      FLEX_COST_CALCULATOR,
      RaptorTestConstants::stopIndexToName,
      null,
      lifeCycle(),
      false
    );
    // When:
    RaptorPath<TestTripSchedule> path = mapper.mapToPath(destArrival);
    // Then:
    assertEquals(expected, path.toStringDetailed(RaptorTestConstants::stopIndexToName));
  }

  private void runTestFlexReverse(
    DestinationArrival<TestTripSchedule> destArrival,
    String expected
  ) {
    // Given:
    var mapper = new ReversePathMapper<TestTripSchedule>(
      FLEX_SLACK_PROVIDER,
      FLEX_COST_CALCULATOR,
      RaptorTestConstants::stopIndexToName,
      null,
      lifeCycle(),
      false
    );
    // When:
    RaptorPath<TestTripSchedule> path = mapper.mapToPath(destArrival);
    // Then:
    assertEquals(expected, path.toStringDetailed(RaptorTestConstants::stopIndexToName));
  }
}
