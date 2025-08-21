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
  private final List<FareOffer> itineraryFareUses;

  public ItineraryFaresDecorator(ItineraryFare fare, List<FareOffer> itineraryFareUses) {
    this.fare = fare;
    this.itineraryFareUses = itineraryFareUses;
  }

  public static Itinerary decorateItineraryWithFare(Itinerary i, ItineraryFare fare) {
    var legDecorator = new ItineraryFaresDecorator(fare, createItineraryFareUses(i, fare));
    return i.copyOf().withFare(fare).transformTransitLegs(legDecorator::decorateTransitLeg).build();
  }

  private static List<FareOffer> createItineraryFareUses(Itinerary i, ItineraryFare fare) {
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
    var legUses = fare.getLegProducts().get(leg);
    var allUses = ListUtils.combine(itineraryFareUses, legUses);

    return (leg instanceof FareProductAware<TransitLeg> fpa)
      ? fpa.decorateWithFareOffers(allUses)
      : leg;
  }
}
