package org.opentripplanner.ext.fares;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.fare.ItineraryFare;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.Money;

public class FaresFilterTest implements PlanTestConstants {

  private final TimetableRepositoryForTest testModel = TimetableRepositoryForTest.of();

  private Itinerary buildItinerary() {
    return newItinerary(A, 0)
      .walk(20, Place.forStop(testModel.stop("1:stop", 1d, 1d).build()))
      .bus(1, 0, 50, B)
      .bus(1, 52, 100, C)
      .build();
  }

  @Test
  void shouldAddFare() {
    Itinerary i1 = buildItinerary();

    assertEquals(ItineraryFare.empty(), i1.fare());

    var fares = new ItineraryFare();

    var leg = i1.legs().get(1);
    var fp = FareProduct.of(id("fp"), "fare product", Money.euros(10.00f)).build();
    fares.addFareProduct(leg, FareOffer.of(leg.startTime(), fp));

    var filter = new DecorateWithFare((FareService) itinerary -> fares);

    i1 = filter.decorate(i1);

    assertEquals(fares, i1.fare());

    var busLeg = i1.transitLeg(1);

    assertEquals(List.of(FareOffer.of(busLeg.startTime(), fp)), busLeg.fareOffers());
  }

  static Stream<Arguments> emptyOrNullFareCases() {
    return Stream.of(
      Arguments.of("empty", (FareService) itinerary -> ItineraryFare.empty()),
      Arguments.of("null", (FareService) itinerary -> null)
    );
  }

  @ParameterizedTest(name = "{0} fare must not trigger an itinerary rebuild")
  @MethodSource("emptyOrNullFareCases")
  void shouldSkipDecorationWhenFareProducesNoProducts(String label, FareService fareService) {
    Itinerary i1 = buildItinerary();

    var decorated = new DecorateWithFare(fareService).decorate(i1);

    assertSame(i1, decorated);
  }
}
