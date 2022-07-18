package org.opentripplanner.transit.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.ext.fares.impl.DefaultFareServiceFactory;

class TransitModelTest {

  @Test
  void validateTimeZones() {
    // First GTFS bundle should be added succesfully
    var model = ConstantsForTests.buildGtfsGraph(ConstantsForTests.FAKE_GTFS);

    // Should throw on second bundle, with different agency time zone
    assertThrows(
      IllegalStateException.class,
      () ->
        ConstantsForTests.addGtfsToGraph(
          model.graph,
          model.transitModel,
          ConstantsForTests.KCM_GTFS,
          new DefaultFareServiceFactory(),
          null
        ),
      "The graph contains agencies with different time zones. Please configure the one to be used in the build-config.json"
    );
  }
}
