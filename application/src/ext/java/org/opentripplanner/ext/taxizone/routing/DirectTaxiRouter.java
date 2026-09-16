package org.opentripplanner.ext.taxizone.routing;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.ext.dataoverlay.configuration.DataOverlayParameterBindings;
import org.opentripplanner.ext.taxizone.TaxiZoneIndex;
import org.opentripplanner.ext.taxizone.model.TaxiZoneLeg;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.DirectStreetRouter;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.lang.Sandbox;

/**
 * Decorates itineraries produced by street routing (direct routing) with taxi zone information.
 * For each {@link TraverseMode#CAR} {@link StreetLeg} it:
 * <ol>
 *   <li>Looks up which taxi zone provider covers the leg's pickup and drop-off coordinates by
 *   querying the {@link TaxiZoneIndex}.
 *   <li>If no provider covers both endpoints the itinerary is removed from the result.
 *   <li>Otherwise the driving leg is replaced by a {@link TaxiZoneLeg} decorated with the first
 *   matching zone.
 * </ol>
 *
 * <p>
 * TODO: Multi-provider support. Currently only the first matching zone is used. In the future
 * all matching providers should be available so users can choose.
 */
@Sandbox
public class DirectTaxiRouter {

  private final TaxiZoneIndex taxiZoneIndex;

  public DirectTaxiRouter(TaxiZoneIndex taxiZoneIndex) {
    this.taxiZoneIndex = taxiZoneIndex;
  }

  /**
   * Routes a direct taxi itinerary by delegating to {@link DirectStreetRouter} and
   * decorating the resulting itineraries with taxi zone information.
   */
  public List<Itinerary> route(
    Graph graph,
    TransitService transitService,
    StreetLimitationParametersService streetLimitationParametersService,
    VehicleRentalService vehicleRentalService,
    StreetDetailsService streetDetailsService,
    @Nullable DataOverlayParameterBindings dataOverlayParameterBindings,
    RouteRequest request,
    LinkingContext linkingContext
  ) {
    var itineraries = DirectStreetRouter.route(
      graph,
      transitService,
      streetLimitationParametersService,
      vehicleRentalService,
      streetDetailsService,
      dataOverlayParameterBindings,
      request,
      linkingContext
    );
    return decorateAndFilter(itineraries);
  }

  List<Itinerary> decorateAndFilter(List<Itinerary> itineraries) {
    List<Itinerary> result = new ArrayList<>();
    for (Itinerary itinerary : itineraries) {
      decorateItinerary(itinerary).ifPresent(result::add);
    }
    return result;
  }

  /**
   * Returns the decorated itinerary, or {@link Optional#empty()} if a {@link TraverseMode#CAR}
   * leg has no matching taxi zone (in which case the whole itinerary is dropped).
   */
  private Optional<Itinerary> decorateItinerary(Itinerary itinerary) {
    List<Leg> newLegs = new ArrayList<>();
    for (Leg leg : itinerary.legs()) {
      if (leg instanceof StreetLeg streetLeg && streetLeg.getMode() == TraverseMode.CAR) {
        var taxiZone = taxiZoneIndex.findFirstZone(
          streetLeg.from().coordinate,
          streetLeg.to().coordinate
        );
        if (taxiZone.isEmpty()) {
          return Optional.empty();
        }
        newLegs.add(new TaxiZoneLeg(streetLeg, taxiZone.get()));
      } else {
        newLegs.add(leg);
      }
    }
    return Optional.of(itinerary.copyOf().withLegs(newLegs).build());
  }
}
