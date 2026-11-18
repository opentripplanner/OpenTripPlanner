package org.opentripplanner.transfer.streetadapter;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.place.nearbystopfinder.PatternConsideringNearbyStopFinder;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transit.service.TransitRepository;

class StreetTransferPathProviderTest {

  /**
   * The feature is not toggled, since the features are global and the tests that build a graph run
   * in parallel. The feature is on by default.
   */
  @Test
  void theNearbyStopsAreLimitedToTheClosestStopOfEachPatternWithTheFeatureOn() {
    var finder = StreetTransferPathProvider.createNearbyStopFinder(
      new Graph(),
      new TransitRepository(),
      stopId -> List.of(),
      null
    );

    assertThat(OTPFeature.ConsiderPatternsForDirectTransfers.isOn()).isTrue();
    assertThat(finder).isInstanceOf(PatternConsideringNearbyStopFinder.class);
  }
}
