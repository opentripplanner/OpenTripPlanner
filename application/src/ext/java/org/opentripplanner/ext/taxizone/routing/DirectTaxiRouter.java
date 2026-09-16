package org.opentripplanner.ext.taxizone.routing;

import java.util.ArrayList;
import java.util.List;
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
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.lang.Sandbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Routes and decorates direct taxi itineraries with taxi zone information.
 * <ol>
 *   <li>Before the street search runs, {@link #route} checks that the request origin and
 *   destination are covered by a common taxi zone provider (via the {@link TaxiZoneIndex}); if
 *   not, an empty result is returned immediately without running {@link DirectStreetRouter}.
 *   <li>Once the street search has produced itineraries, each {@link TraverseMode#CAR}
 *   {@link StreetLeg} is replaced by a {@link TaxiZoneLeg} decorated with the matching zone,
 *   looked up using the same logical (request origin/destination) coordinates, rather than the
 *   leg's own local coordinates, which may differ slightly when the route is a walk-drive-walk
 *   chain.
 * </ol>
 *
 * <p>
 * TODO: Multi-provider support. Currently only the first matching zone is used. In the future
 * all matching providers should be available so users can choose.
 */
@Sandbox
public class DirectTaxiRouter {

  private static final Logger LOG = LoggerFactory.getLogger(DirectTaxiRouter.class);

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
    WgsCoordinate pickup = request.from().wgsCoordinate();
    WgsCoordinate dropoff = request.to().wgsCoordinate();
    if (taxiZoneIndex.findFirstZone(pickup, dropoff).isEmpty()) {
      return List.of();
    }

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
    return decorate(itineraries, pickup, dropoff);
  }

  List<Itinerary> decorate(
    List<Itinerary> itineraries,
    WgsCoordinate pickup,
    WgsCoordinate dropoff
  ) {
    List<Itinerary> result = new ArrayList<>(itineraries.size());
    for (Itinerary itinerary : itineraries) {
      result.add(decorateItinerary(itinerary, pickup, dropoff));
    }
    return result;
  }

  /**
   * Decorates every {@link TraverseMode#CAR} leg in the itinerary with taxi zone information,
   * looking up the zone using the given logical {@code pickup}/{@code dropoff} coordinates rather
   * than the leg's own local coordinates.
   */
  private Itinerary decorateItinerary(
    Itinerary itinerary,
    WgsCoordinate pickup,
    WgsCoordinate dropoff
  ) {
    List<Leg> newLegs = new ArrayList<>();
    for (Leg leg : itinerary.legs()) {
      if (leg instanceof StreetLeg streetLeg && streetLeg.getMode() == TraverseMode.CAR) {
        var taxiZone = taxiZoneIndex.findFirstZone(pickup, dropoff);
        if (taxiZone.isEmpty()) {
          LOG.warn(
            "No taxi zone covers the pre-filtered direct taxi leg between {} and {}",
            pickup,
            dropoff
          );
          newLegs.add(leg);
        } else {
          newLegs.add(new TaxiZoneLeg(streetLeg, taxiZone.get()));
        }
      } else {
        newLegs.add(leg);
      }
    }
    return itinerary.copyOf().withLegs(newLegs).build();
  }
}
