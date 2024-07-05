package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMap;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMapById;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.DefaultEntityById;
import org.opentripplanner.transit.model.timetable.Trip;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.rutebanken.netex.model.TimetabledPassingTime;

public class StopTimesMapperTest {

  private static final BigInteger ZERO = BigInteger.valueOf(0);
  private static final BigInteger ONE = BigInteger.valueOf(1);
  private static final BigInteger TWO = BigInteger.valueOf(2);

  private static final LocalTime QUARTER_PAST_FIVE = LocalTime.of(5, 15);
  public static final Trip TRIP = TransitModelForTest.trip("T1").build();

  @Test
  public void testCalculateOtpTime() {
    assertEquals(18900, StopTimesMapper.calculateOtpTime(QUARTER_PAST_FIVE, ZERO));
    assertEquals(105300, StopTimesMapper.calculateOtpTime(QUARTER_PAST_FIVE, ONE));
    assertEquals(191700, StopTimesMapper.calculateOtpTime(QUARTER_PAST_FIVE, TWO));
  }

  @Test
  public void testMapStopTimes() {
    NetexTestDataSample sample = new NetexTestDataSample();

    StopTimesMapper stopTimesMapper = new StopTimesMapper(
      DataImportIssueStore.NOOP,
      MappingSupport.ID_FACTORY,
      sample.getStopsById(),
      new DefaultEntityById<>(),
      new DefaultEntityById<>(),
      sample.getDestinationDisplayById(),
      sample.getQuayIdByStopPointRef(),
      new HierarchicalMap<>(),
      new HierarchicalMapById<>(),
      new HierarchicalMap<>()
    );

    StopTimesMapperResult result = stopTimesMapper.mapToStopTimes(
      sample.getJourneyPattern(),
      TRIP,
      sample.getTimetabledPassingTimes(),
      null
    );

    List<StopTime> stopTimes = result.stopTimes;

    assertEquals(4, stopTimes.size());

    assertStop(stopTimes.get(0), "NSR:Quay:1", 18000, "Bergen", List.of("Stavanger"));
    assertStop(stopTimes.get(1), "NSR:Quay:2", 18240, "Bergen", List.of("Stavanger"));
    assertStop(stopTimes.get(2), "NSR:Quay:3", 18600, "Stavanger", List.of("Bergen"));
    assertStop(stopTimes.get(3), "NSR:Quay:4", 18900, "Stavanger", List.of("Bergen"));

    Map<String, StopTime> map = result.stopTimeByNetexId;

    assertEquals(stopTimes.get(0), map.get("TTPT-1"));
    assertEquals(stopTimes.get(1), map.get("TTPT-2"));
    assertEquals(stopTimes.get(2), map.get("TTPT-3"));
    assertEquals(stopTimes.get(3), map.get("TTPT-4"));
  }

  /**
   * Test StopTime.timepoint mapping from NeTEx. Should be true if StopPointInJourneyPattern.isIsWaitPoint
   * is true and corresponding TimetabledPassingTime.waitingTime is defined
   * <p>
   * The sample has 4 StopPointInJourneyPattern points with corresponding TimetabledPassingTime
   * objects. The two last StopPointInJourneyPattern has withIsWaitPoint and undefined, so they
   * should not set StopTime.timepoint = 1. The second TimetabledPassingTime.WaitingTime is
   * undefined so it should also not set StopTime.timepoint = 1. Only the first StopTime has the
   * correct criteria to map StopTime.timepoint = 1.
   */
  @Test
  public void testWaitPointMapping() {
    var netexSample = new NetexTestDataSample();

    var points = netexSample
      .getJourneyPattern()
      .getPointsInSequence()
      .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
      .stream()
      .map(StopPointInJourneyPattern.class::cast)
      .collect(Collectors.toList());

    assertEquals(4, points.size(), "Expected StopPointInJourneyPattern.size to be 4");

    points.get(0).withIsWaitPoint(true);
    points.get(1).withIsWaitPoint(true);
    points.get(2).withIsWaitPoint(false);
    points.get(3).withIsWaitPoint(null);

    var passingTimes = netexSample.getTimetabledPassingTimes();
    assertEquals(
      points.size(),
      passingTimes.size(),
      "Expected TimetabledPassingTimes size equal StopPointInJourneyPattern size"
    );

    // Utility function to find TimetabledPassingTime by StopPointInJourneyPattern.Id
    Function<String, TimetabledPassingTime> findPassingTime = pointId ->
      passingTimes
        .stream()
        .filter(t -> pointId.equals(t.getPointInJourneyPatternRef().getValue().getRef()))
        .findAny()
        .orElseThrow();

    var firstPassingTime = findPassingTime.apply(points.get(0).getId());
    var thirdPassingTime = findPassingTime.apply(points.get(2).getId());
    var fourthPassingTime = findPassingTime.apply(points.get(3).getId());

    // Make TimeTabledPassingTime valid waitPoint
    firstPassingTime.setWaitingTime(Duration.ofSeconds(0));
    thirdPassingTime.setWaitingTime(Duration.ofSeconds(10));
    fourthPassingTime.setWaitingTime(Duration.ofSeconds(-5));

    StopTimesMapper stopTimesMapper = new StopTimesMapper(
      DataImportIssueStore.NOOP,
      MappingSupport.ID_FACTORY,
      netexSample.getStopsById(),
      new DefaultEntityById<>(),
      new DefaultEntityById<>(),
      netexSample.getDestinationDisplayById(),
      netexSample.getQuayIdByStopPointRef(),
      new HierarchicalMap<>(),
      new HierarchicalMapById<>(),
      new HierarchicalMap<>()
    );

    StopTimesMapperResult result = stopTimesMapper.mapToStopTimes(
      netexSample.getJourneyPattern(),
      TRIP,
      netexSample.getTimetabledPassingTimes(),
      null
    );

    assertNotNull(result, "result must not be null");

    var stopTimes = result.stopTimes;

    assertEquals(4, stopTimes.size(), "Exptected 4 StopTime objects");

    Assertions.assertAll(
      () -> assertEquals(1, stopTimes.get(0).getTimepoint(), "StopTime expected to be waitPoint"),
      () ->
        assertNotEquals(
          1,
          stopTimes.get(1).getTimepoint(),
          "StopTime expected to not be waitPoint"
        ),
      () ->
        assertNotEquals(
          1,
          stopTimes.get(2).getTimepoint(),
          "StopTime expected to not be waitPoint"
        ),
      () ->
        assertNotEquals(1, stopTimes.get(3).getTimepoint(), "StopTime expected to not be waitPoint")
    );
  }

  private void assertStop(
    StopTime stopTime,
    String stopId,
    long departureTime,
    String headsign,
    List<String> via
  ) {
    assertEquals(stopId, stopTime.getStop().getId().getId());
    assertEquals(departureTime, stopTime.getDepartureTime());
    assertEquals(headsign, stopTime.getStopHeadsign().toString());

    List<String> stopTimeVia = stopTime.getHeadsignVias();
    assertEquals(via, stopTimeVia);
  }
}
