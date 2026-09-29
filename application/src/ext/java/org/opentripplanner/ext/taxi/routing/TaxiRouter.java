package org.opentripplanner.ext.taxi.routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.ext.dataoverlay.configuration.DataOverlayParameterBindings;
import org.opentripplanner.ext.taxi.TaxiRouteIndex;
import org.opentripplanner.ext.taxi.model.TaxiLeg;
import org.opentripplanner.ext.taxi.model.TaxiRoute;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.DirectStreetRouter;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.lang.Sandbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles taxi routing, filtering and decoration, backed by a single {@link TaxiRouteIndex}.
 * <ol>
 *   <li>Before a direct taxi street search runs, {@link #routeDirect} checks that the request
 *   origin and destination are covered by a common taxi provider; if not, an empty result
 *   is returned immediately without running {@link DirectStreetRouter}.
 *   <li>Before a transit search runs, {@link #filterNearbyStops} drops candidate access/egress
 *   stops whose logical endpoints (the request origin/destination and the stop) are not covered
 *   by a common taxi provider, so RAPTOR never considers a combination that would later be
 *   rejected.
 *   <li>Once a search has produced results, {@link #decorateItineraries} (direct routing) and
 *   {@link #decorateAccessEgressLegs} (transit access/egress) replace each {@link
 *   TraverseMode#CAR} {@link StreetLeg} with a {@link TaxiLeg} decorated with the matching
 *   provider, looked up using the same logical coordinates rather than the leg's own local
 *   coordinates, which may differ slightly when the route is a walk-drive-walk chain.
 * </ol>
 *
 * <p>
 * TODO: Multi-provider support. Currently only the first matching provider is used. In the future
 * all matching providers should be available so users can choose.
 */
@Sandbox
public class TaxiRouter {

  private static final Logger LOG = LoggerFactory.getLogger(TaxiRouter.class);

  private final TaxiRouteIndex taxiRouteIndex;

  public TaxiRouter(TaxiRouteIndex taxiRouteIndex) {
    this.taxiRouteIndex = taxiRouteIndex;
  }

  /**
   * Routes a direct taxi itinerary by delegating to {@link DirectStreetRouter} and
   * decorating the resulting itineraries with taxi provider information.
   */
  public List<Itinerary> routeDirect(
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
    if (taxiRouteIndex.findFirstRoute(pickup, dropoff).isEmpty()) {
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
    return decorateItineraries(itineraries, pickup, dropoff);
  }

  List<Itinerary> decorateItineraries(
    List<Itinerary> itineraries,
    WgsCoordinate pickup,
    WgsCoordinate dropoff
  ) {
    List<Itinerary> result = new ArrayList<>(itineraries.size());
    for (Itinerary itinerary : itineraries) {
      var newLegs = decorateLegs(itinerary.legs(), pickup, dropoff);
      result.add(itinerary.copyOf().withLegs(newLegs).build());
    }
    return result;
  }

  /**
   * Drops access/egress candidates whose logical endpoints (the request origin/destination and
   * the stop) are not covered by a common taxi provider.
   */
  public Collection<NearbyStop> filterNearbyStops(
    TransitService transitService,
    Collection<NearbyStop> nearbyStops,
    AccessEgressType type,
    RouteRequest request
  ) {
    return type.isAccess()
      ? filterNearbyStops(transitService, nearbyStops, request.from().wgsCoordinate())
      : filterNearbyStops(transitService, nearbyStops, request.to().wgsCoordinate());
  }

  private Collection<NearbyStop> filterNearbyStops(
    TransitService transitService,
    Collection<NearbyStop> nearbyStops,
    WgsCoordinate coordinate
  ) {
    List<TaxiRoute> routes = taxiRouteIndex.findAllRoutes(coordinate);
    if (routes.isEmpty()) {
      return List.of();
    }

    List<NearbyStop> result = new ArrayList<>(nearbyStops.size());
    for (NearbyStop nearbyStop : nearbyStops) {
      Point stopPoint = GeometryUtils.getGeometryFactory().createPoint(
        transitService.getStopLocation(nearbyStop.stopId).getCoordinate().asJtsCoordinate()
      );
      for (TaxiRoute route : routes) {
        if (taxiRouteIndex.getPreparedGeometry(route).contains(stopPoint)) {
          result.add(nearbyStop);
          break;
        }
      }
    }
    return result;
  }

  /**
   * Decorates the {@link TraverseMode#CAR} leg among an access or egress leg chain with taxi
   * route information, looking up the route using the given logical {@code pickup} and
   * {@code dropoff} coordinates (the request origin/destination and the stop), rather than the
   * leg's own local coordinates, which may differ slightly when the access/egress path is a
   * walk-drive-walk chain.
   * <p>
   * Candidates are expected to already have been filtered for route coverage (see
   * {@link #filterNearbyStops}), so a common route is expected to always exist; if none is found
   * (defensive) the leg is returned unchanged and a warning is logged.
   */
  public List<Leg> decorateAccessEgressLegs(
    List<Leg> legs,
    WgsCoordinate pickup,
    WgsCoordinate dropoff
  ) {
    return decorateLegs(legs, pickup, dropoff);
  }

  /**
   * Replaces every {@link TraverseMode#CAR} {@link StreetLeg} among {@code legs} with a {@link
   * TaxiLeg}, looking up the covering route via {@code pickup}/{@code dropoff} once (not per
   * leg, since they're invariant across the whole call) and reusing the result for every
   * matching leg.
   */
  private List<Leg> decorateLegs(List<Leg> legs, WgsCoordinate pickup, WgsCoordinate dropoff) {
    var taxiRoute = taxiRouteIndex.findFirstRoute(pickup, dropoff);
    if (taxiRoute.isEmpty()) {
      LOG.warn(
        "No taxi provider covers the pre-filtered taxi legs between {} and {}",
        pickup,
        dropoff
      );
      return legs;
    }

    List<Leg> newLegs = new ArrayList<>(legs.size());
    for (Leg leg : legs) {
      if (leg instanceof StreetLeg streetLeg && streetLeg.getMode() == TraverseMode.CAR) {
        newLegs.add(new TaxiLeg(streetLeg, taxiRoute.get()));
      } else {
        newLegs.add(leg);
      }
    }
    return newLegs;
  }
}
