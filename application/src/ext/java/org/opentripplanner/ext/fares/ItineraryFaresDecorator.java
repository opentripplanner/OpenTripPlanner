package org.opentripplanner.ext.fares;

import java.util.List;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.fare.ItineraryFare;
import org.opentripplanner.model.plan.FareProductAware;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.TransitLeg;
import org.opentripplanner.utils.collection.ListUtils;

/**
 * Takes fares and applies them to the legs of an itinerary.
 */
public final class ItineraryFaresDecorator {

  private final ItineraryFare fare;
  private final List<FareOffer> fareOffers;

  public ItineraryFaresDecorator(ItineraryFare fare, List<FareOffer> fareOffers) {
    this.fare = fare;
    this.fareOffers = fareOffers;
  }

  public static Itinerary decorateItineraryWithFare(Itinerary i, ItineraryFare fare) {
    var legDecorator = new ItineraryFaresDecorator(fare, createFareOffers(i, fare));
    return i.copyOf().withFare(fare).transformTransitLegs(legDecorator::decorateTransitLeg).build();
  }

  private static List<FareOffer> createFareOffers(Itinerary i, ItineraryFare fare) {
    return fare
      .getItineraryProducts()
      .stream()
      .map(fp -> {
        var startTime = i.legs().getFirst().startTime();
        return FareOffer.of(startTime, fp);
      })
      .toList();
  }

  private TransitLeg decorateTransitLeg(TransitLeg leg) {
    var legOffers = fare.getLegProducts().get(leg);
    var allOfferes = ListUtils.combine(fareOffers, legOffers);

    return (leg instanceof FareProductAware<TransitLeg> fpa)
      ? fpa.decorateWithFareOffers(allOfferes)
      : leg;
  }
}
