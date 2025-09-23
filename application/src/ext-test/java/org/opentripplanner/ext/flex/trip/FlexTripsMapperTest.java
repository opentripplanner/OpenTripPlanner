package org.opentripplanner.ext.flex.trip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.opentripplanner.ext.flex.FlexStopTimesForTest.area;
import static org.opentripplanner.graph_builder.issue.api.DataImportIssueStore.NOOP;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.flex.FlexStopTimesForTest;
import org.opentripplanner.ext.flex.FlexTripsMapper;
import org.opentripplanner.ext.flex.flexpathcalculator.DirectFlexPathCalculator;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.transit.service.SiteRepository;

class FlexTripsMapperTest {

  @Test
  void defaultTimePenalty() {
    var builder = new OtpTransitServiceBuilder(SiteRepository.of().build(), NOOP);
    builder
      .getStopTimesSortedByTrip()
      .addAll(List.of(area("10:00", "18:00"), area("10:00", "18:00")));
    var trips = FlexTripsMapper.createFlexTrips(builder, NOOP);
    assertEquals("[UnscheduledTrip{F:flex}]", trips.toString());
    var unscheduled = (UnscheduledTrip) trips.getFirst();
    var unchanged = unscheduled.decorateFlexPathCalculator(new DirectFlexPathCalculator());
    assertInstanceOf(DirectFlexPathCalculator.class, unchanged);
  }

  /**
   * Checks that a trip with a single stop time is not mapped to a flex trip and an issue is reported.
   */
  @Test
  void tooFewStopTimes() {
    var issueStore = new DefaultDataImportIssueStore();
    var builder = new OtpTransitServiceBuilder(SiteRepository.of().build(), issueStore);
    builder.getStopTimesSortedByTrip().addAll(List.of(area("10:00", "18:00")));
    var trips = FlexTripsMapper.createFlexTrips(builder, issueStore);
    assertEquals(0, trips.size());
    assertEquals(
      "[Issue{type: 'InvalidFlexTrip', message: 'Trip F:flex defines only a single stop time, which is invalid: https://gtfs.org/documentation/schedule/examples/flex/'}]",
      issueStore.listIssues().toString()
    );
  }
}
