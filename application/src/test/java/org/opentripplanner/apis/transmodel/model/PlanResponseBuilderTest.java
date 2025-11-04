package org.opentripplanner.apis.transmodel.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.model.plan.TripPlan;
import org.opentripplanner.model.plan.paging.cursor.PageCursor;
import org.opentripplanner.model.plan.paging.cursor.PageType;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.api.response.TripSearchMetadata;

class PlanResponseBuilderTest {

  private static final Instant TEST_DATE = Instant.parse("2024-01-15T10:00:00Z");
  private static final Place FROM_PLACE = Place.normal(
    60.0,
    10.0,
    new NonLocalizedString("From Place")
  );
  private static final Place TO_PLACE = Place.normal(
    60.1,
    10.1,
    new NonLocalizedString("To Place")
  );

  @Test
  void buildWithMinimalData() {
    TripPlan tripPlan = createTripPlan(List.of());

    PlanResponse response = PlanResponse.of().withPlan(tripPlan).build();

    assertNotNull(response);
    assertEquals(TEST_DATE, response.date());
    assertEquals(FROM_PLACE, response.from());
    assertEquals(TO_PLACE, response.to());
    assertTrue(response.itineraries().isEmpty());
    assertTrue(response.messages().isEmpty());
    assertNull(response.metadata());
    assertNull(response.debugOutput());
    assertNull(response.previousPageCursor());
    assertNull(response.nextPageCursor());
  }

  @Test
  void buildWithAllData() {
    TripPlan tripPlan = createTripPlan(List.of());
    TripSearchMetadata metadata = TripSearchMetadata.createForDepartAfter(
      TEST_DATE,
      Duration.ofHours(2),
      TEST_DATE.plus(Duration.ofHours(1))
    );
    List<RoutingError> messages = List.of(
      new RoutingError(RoutingErrorCode.NO_TRANSIT_CONNECTION, InputField.DATE_TIME)
    );
    PageCursor previousCursor = createPageCursor(PageType.PREVIOUS_PAGE);
    PageCursor nextCursor = createPageCursor(PageType.NEXT_PAGE);

    PlanResponse response = PlanResponse.of()
      .withPlan(tripPlan)
      .withMetadata(metadata)
      .withMessages(messages)
      .withDebugOutput(null)
      .withPreviousPageCursor(previousCursor)
      .withNextPageCursor(nextCursor)
      .build();

    assertNotNull(response);
    assertEquals(TEST_DATE, response.date());
    assertEquals(FROM_PLACE, response.from());
    assertEquals(TO_PLACE, response.to());
    assertEquals(metadata, response.metadata());
    assertEquals(1, response.messages().size());
    assertEquals(RoutingErrorCode.NO_TRANSIT_CONNECTION, response.messages().get(0).code);
    assertNull(response.debugOutput());
    assertEquals(previousCursor, response.previousPageCursor());
    assertEquals(nextCursor, response.nextPageCursor());
  }

  private TripPlan createTripPlan(List<Itinerary> itineraries) {
    return new TripPlan(FROM_PLACE, TO_PLACE, TEST_DATE, itineraries);
  }

  private PageCursor createPageCursor(PageType pageType) {
    return new PageCursor(
      pageType,
      SortOrder.STREET_AND_ARRIVAL_TIME,
      TEST_DATE,
      TEST_DATE.plus(Duration.ofHours(2)),
      Duration.ofHours(2),
      null,
      null
    );
  }
}
