package org.opentripplanner.raptor.rangeraptor.path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.stoparrival.BasicPathTestCase.BASIC_PATH_AS_DETAILED_STRING;
import static org.opentripplanner.raptor._data.stoparrival.BasicPathTestCase.COST_CALCULATOR;
import static org.opentripplanner.raptor._data.stoparrival.BasicPathTestCase.SLACK_PROVIDER;
import static org.opentripplanner.raptor._data.stoparrival.BasicPathTestCase.basicTripByForwardSearch;
import static org.opentripplanner.raptor._data.stoparrival.BasicPathTestCase.basicTripByReverseSearch;
import static org.opentripplanner.raptor._data.stoparrival.BasicPathTestCase.lifeCycle;
import static org.opentripplanner.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.flexCaseAForwardSearch;
import static org.opentripplanner.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.flexCaseAReverseSearch;
import static org.opentripplanner.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.flexCaseAText;
import static org.opentripplanner.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.flexCaseAWithOpeningHoursForwardSearch;
import static org.opentripplanner.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.flexCaseAWithOpeningHoursReverseSearch;
import static org.opentripplanner.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.flexCaseAWithOpeningHoursText;
import static org.opentripplanner.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.flexCaseBForwardSearch;
import static org.opentripplanner.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.flexCaseBReverseSearch;
import static org.opentripplanner.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.flexCaseBText;
import static org.opentripplanner.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.flexCaseBWithOpeningHoursForwardSearch;
import static org.opentripplanner.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.flexCaseBWithOpeningHoursReverseSearch;
import static org.opentripplanner.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.flexCaseBWithOpeningHoursText;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.path.Path;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.DefaultCostCalculator;

public class PathMapperTest implements RaptorTestConstants {

  private static final RaptorSlackProvider FLEX_SLACK_PROVIDER =
    FlexAccessAndEgressPathTestCase.SLACK_PROVIDER;
  private static final DefaultCostCalculator FLEX_COST_CALCULATOR =
    FlexAccessAndEgressPathTestCase.COST_CALCULATOR;

  /* BASIC CASES */

  @Test
  public void mapToPathBasicForwardSearch() {
    // Given:
    var destArrival = basicTripByForwardSearch();
    var mapper = new ForwardPathMapper<TestTripSchedule>(
      null,
      SLACK_PROVIDER,
      COST_CALCULATOR,
      this::stopIndexToName,
      lifeCycle(),
      false
    );

    //When:
    Path<TestTripSchedule> path = mapper.mapToPath(destArrival);

    // Then: verify path - should be the same for reverse and forward mapper
    assertPath(path);
  }

  @Test
  public void mapToPathBasicReverseSearch() {
    // Given:
    var destArrival = basicTripByReverseSearch();
    var mapper = new ReversePathMapper<TestTripSchedule>(
      null,
      SLACK_PROVIDER,
      COST_CALCULATOR,
      this::stopIndexToName,
      lifeCycle(),
      false
    );

    //When:
    Path<TestTripSchedule> path = mapper.mapToPath(destArrival);

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

  private void assertPath(Path<TestTripSchedule> path) {
    assertEquals(BASIC_PATH_AS_DETAILED_STRING, path.toStringDetailed(this::stopIndexToName));
  }

  private void runTestFlexForward(
    DestinationArrival<TestTripSchedule> destArrival,
    String expected
  ) {
    // Given:
    var mapper = new ForwardPathMapper<TestTripSchedule>(
      null,
      FLEX_SLACK_PROVIDER,
      FLEX_COST_CALCULATOR,
      this::stopIndexToName,
      lifeCycle(),
      false
    );
    // When:
    Path<TestTripSchedule> path = mapper.mapToPath(destArrival);
    // Then:
    assertEquals(expected, path.toStringDetailed(this::stopIndexToName));
  }

  private void runTestFlexReverse(
    DestinationArrival<TestTripSchedule> destArrival,
    String expected
  ) {
    // Given:
    var mapper = new ReversePathMapper<TestTripSchedule>(
      null,
      FLEX_SLACK_PROVIDER,
      FLEX_COST_CALCULATOR,
      this::stopIndexToName,
      lifeCycle(),
      false
    );
    // When:
    Path<TestTripSchedule> path = mapper.mapToPath(destArrival);
    // Then:
    assertEquals(expected, path.toStringDetailed(this::stopIndexToName));
  }
}
