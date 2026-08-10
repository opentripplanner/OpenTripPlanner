package org.opentripplanner.ext.stopconsolidation;

import java.util.List;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.TransitRepository;

class TestStopConsolidationModel {

  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();
  public static final RegularStop STOP_A = TEST_MODEL.stop("A").withCoordinate(1, 1).build();
  public static final RegularStop STOP_B = TEST_MODEL.stop("B").withCoordinate(1.1, 1.1).build();
  public static final RegularStop STOP_C = TEST_MODEL.stop("C").withCoordinate(1.2, 1.2).build();
  public static final StopPattern STOP_PATTERN = TransitRepositoryForTest.stopPattern(
    STOP_A,
    STOP_B,
    STOP_C
  );
  static final String SECONDARY_FEED_ID = "secondary";
  static final Agency AGENCY = TransitRepositoryForTest.agency("agency")
    .copy()
    .withId(new FeedScopedId(SECONDARY_FEED_ID, "agency"))
    .build();
  static final Route ROUTE = TransitRepositoryForTest.route(
    new FeedScopedId(SECONDARY_FEED_ID, "route-33")
  )
    .withAgency(AGENCY)
    .build();
  static final RegularStop STOP_D = TEST_MODEL.stop("D")
    .withId(new FeedScopedId(SECONDARY_FEED_ID, "secondary-stop-D"))
    .build();

  static final TripPattern PATTERN = TripPattern.of(new FeedScopedId(SECONDARY_FEED_ID, "123"))
    .withRoute(ROUTE)
    .withStopPattern(STOP_PATTERN)
    .build();

  static TransitRepository buildTransitRepository() {
    var siteRepositoryBuilder = TEST_MODEL.siteRepositoryBuilder();
    List.of(STOP_A, STOP_B, STOP_C, STOP_D).forEach(siteRepositoryBuilder::withRegularStop);
    return new TransitRepository(siteRepositoryBuilder.build());
  }
}
