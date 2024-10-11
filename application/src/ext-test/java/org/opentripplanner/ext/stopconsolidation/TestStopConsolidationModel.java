package org.opentripplanner.ext.stopconsolidation;

import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import java.util.List;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.TransitModel;

class TestStopConsolidationModel {

  private static final TransitModelForTest testModel = TransitModelForTest.of();
  public static final RegularStop STOP_A = testModel.stop("A").withCoordinate(1, 1).build();
  public static final RegularStop STOP_B = testModel.stop("B").withCoordinate(1.1, 1.1).build();
  public static final RegularStop STOP_C = testModel.stop("C").withCoordinate(1.2, 1.2).build();
  public static final StopPattern STOP_PATTERN = TransitModelForTest.stopPattern(
    STOP_A,
    STOP_B,
    STOP_C
  );
  static final String SECONDARY_FEED_ID = "secondary";
  static final Agency AGENCY = TransitModelForTest
    .agency("agency")
    .copy()
    .withId(new FeedScopedId(SECONDARY_FEED_ID, "agency"))
    .build();
  static final Route ROUTE = TransitModelForTest
    .route(new FeedScopedId(SECONDARY_FEED_ID, "route-33"))
    .withAgency(AGENCY)
    .build();
  static final RegularStop STOP_D = testModel
    .stop("D")
    .withId(new FeedScopedId(SECONDARY_FEED_ID, "secondary-stop-D"))
    .build();

  static final TripPattern PATTERN = TripPattern
    .of(id("123"))
    .withRoute(ROUTE)
    .withStopPattern(STOP_PATTERN)
    .build();

  static TransitModel buildTransitModel() {
    var stopModelBuilder = testModel.stopModelBuilder();
    List.of(STOP_A, STOP_B, STOP_C, STOP_D).forEach(stopModelBuilder::withRegularStop);
    return new TransitModel(stopModelBuilder.build(), new Deduplicator());
  }
}
