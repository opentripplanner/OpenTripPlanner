package org.opentripplanner.ext.flex.trip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.opentripplanner.graph_builder.issue.api.DataImportIssueStore.NOOP;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.flex.FlexStopTimesForTest;
import org.opentripplanner.ext.flex.FlexTripsMapper;
import org.opentripplanner.ext.flex.flexpathcalculator.DirectFlexPathCalculator;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.service.StopModel;

class FlexTripsMapperTest {

  @Test
  void defaultTimePenalty() {
    var builder = new OtpTransitServiceBuilder(StopModel.of().build(), NOOP);
    var stopTimes = List.of(stopTime(0), stopTime(1));
    builder.getStopTimesSortedByTrip().addAll(stopTimes);
    var trips = FlexTripsMapper.createFlexTrips(builder, NOOP);
    assertEquals("[UnscheduledTrip{F:flex-1}]", trips.toString());
    var unscheduled = (UnscheduledTrip) trips.getFirst();
    var unchanged = unscheduled.decorateFlexPathCalculator(new DirectFlexPathCalculator());
    assertInstanceOf(DirectFlexPathCalculator.class, unchanged);
  }

  private static StopTime stopTime(int seq) {
    var st = FlexStopTimesForTest.area("08:00", "18:00");
    st.setTrip(TransitModelForTest.trip("flex-1").build());
    st.setStopSequence(seq);
    return st;
  }
}
