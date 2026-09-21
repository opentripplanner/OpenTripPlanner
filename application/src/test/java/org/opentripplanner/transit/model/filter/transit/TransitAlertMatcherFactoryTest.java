package org.opentripplanner.transit.model.filter.transit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import java.time.Instant;
import java.util.List;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.core.model.time.TimePeriod;
import org.opentripplanner.routing.alertpatch.AlertCalendar;
import org.opentripplanner.routing.alertpatch.AlertCause;
import org.opentripplanner.routing.alertpatch.AlertEffect;
import org.opentripplanner.routing.alertpatch.AlertSeverity;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.api.request.TransitAlertRequest;
import org.opentripplanner.transit.model.filter.selector.FilterRequest;

class TransitAlertMatcherFactoryTest {

  private static final FeedScopedId ROUTE_ID = id("F:R1");
  private static final FeedScopedId STOP_ID = id("F:S1");

  private TransitAlert alert() {
    return TransitAlert.of(id("F:A1"))
      .addEntity(new EntitySelector.Route(ROUTE_ID))
      .addEntity(new EntitySelector.Stop(STOP_ID))
      .withSeverity(AlertSeverity.SEVERE)
      .withCause(AlertCause.WEATHER)
      .withEffect(AlertEffect.NO_SERVICE)
      .withCalendar(
        AlertCalendar.of(TimePeriod.of(Instant.ofEpochSecond(0), Instant.ofEpochSecond(1_000)))
      )
      .build();
  }

  @Test
  void selectFeedMatches() {
    var request = request(select -> select.withFeeds(List.of("F")), null);
    assertTrue(TransitAlertMatcherFactory.of(request).match(alert()));
  }

  @Test
  void selectFeedRejects() {
    var request = request(select -> select.withFeeds(List.of("OTHER")), null);
    assertFalse(TransitAlertMatcherFactory.of(request).match(alert()));
  }

  @Test
  void notCauseRejects() {
    var request = request(null, select -> select.withCauses(List.of(AlertCause.WEATHER)));
    assertFalse(TransitAlertMatcherFactory.of(request).match(alert()));
  }

  @Test
  void dimensionsWithinASelectorAreCombinedWithAnd() {
    var matching = request(
      select ->
        select
          .withSeverityLevels(List.of(AlertSeverity.SEVERE))
          .withCauses(List.of(AlertCause.WEATHER))
          .withEffects(List.of(AlertEffect.NO_SERVICE)),
      null
    );
    assertTrue(TransitAlertMatcherFactory.of(matching).match(alert()));

    var notMatching = request(
      select ->
        select
          .withSeverityLevels(List.of(AlertSeverity.SEVERE))
          .withCauses(List.of(AlertCause.ACCIDENT)),
      null
    );
    assertFalse(TransitAlertMatcherFactory.of(notMatching).match(alert()));
  }

  @Test
  void selectorsAreCombinedWithOr() {
    var request = TransitAlertRequest.of()
      .withFilters(
        List.of(
          FilterRequest.<TransitAlertSelectRequest>of()
            .addSelect(
              TransitAlertSelectRequest.of().withCauses(List.of(AlertCause.ACCIDENT)).build()
            )
            .addSelect(
              TransitAlertSelectRequest.of().withCauses(List.of(AlertCause.WEATHER)).build()
            )
            .build()
        )
      )
      .build();
    assertTrue(TransitAlertMatcherFactory.of(request).match(alert()));
  }

  @Test
  void timePeriodMatches() {
    var range = TimePeriod.of(Instant.ofEpochSecond(0), Instant.ofEpochSecond(2_000));
    var request = request(select -> select.withTimePeriods(List.of(range)), null);
    assertTrue(TransitAlertMatcherFactory.of(request).match(alert()));
  }

  @Test
  void emptyRequestMatchesEverything() {
    var request = TransitAlertRequest.of().build();
    assertTrue(TransitAlertMatcherFactory.of(request).match(alert()));
  }

  private static TransitAlertRequest request(
    @Nullable UnaryOperator<TransitAlertSelectRequest.Builder> select,
    @Nullable UnaryOperator<TransitAlertSelectRequest.Builder> not
  ) {
    var filter = FilterRequest.<TransitAlertSelectRequest>of();
    if (select != null) {
      filter.addSelect(select.apply(TransitAlertSelectRequest.of()).build());
    }
    if (not != null) {
      filter.addNot(not.apply(TransitAlertSelectRequest.of()).build());
    }
    return TransitAlertRequest.of().withFilters(List.of(filter.build())).build();
  }
}
