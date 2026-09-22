package org.opentripplanner.ext.taxi.internal;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.ext.dataoverlay.configuration.DataOverlayParameterBindings;
import org.opentripplanner.ext.taxi.TaxiRouteIndex;
import org.opentripplanner.ext.taxi.TaxiService;
import org.opentripplanner.ext.taxi.model.TaxiRoute;
import org.opentripplanner.ext.taxi.routing.TaxiRouter;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transit.service.TransitService;

public class DefaultTaxiService implements TaxiService {

  private final TaxiRouteIndex taxiRouteIndex;
  private final TaxiRouter taxiRouter;
  private final Graph graph;
  private final StreetLimitationParametersService streetLimitationParametersService;
  private final VehicleRentalService vehicleRentalService;
  private final StreetDetailsService streetDetailsService;

  @Nullable
  private final DataOverlayParameterBindings dataOverlayParameterBindings;

  public DefaultTaxiService(
    List<TaxiRoute> routes,
    Graph graph,
    StreetLimitationParametersService streetLimitationParametersService,
    VehicleRentalService vehicleRentalService,
    StreetDetailsService streetDetailsService,
    @Nullable DataOverlayParameterBindings dataOverlayParameterBindings
  ) {
    this.taxiRouteIndex = TaxiRouteIndex.createAndIndex(routes);
    this.taxiRouter = new TaxiRouter(taxiRouteIndex);
    this.graph = Objects.requireNonNull(graph);
    this.streetLimitationParametersService = Objects.requireNonNull(
      streetLimitationParametersService
    );
    this.vehicleRentalService = Objects.requireNonNull(vehicleRentalService);
    this.streetDetailsService = Objects.requireNonNull(streetDetailsService);
    this.dataOverlayParameterBindings = dataOverlayParameterBindings;
  }

  @Override
  public Collection<NearbyStop> filterNearbyStops(
    TransitService transitService,
    Collection<NearbyStop> nearbyStops,
    AccessEgressType type,
    RouteRequest request
  ) {
    return taxiRouter.filterNearbyStops(transitService, nearbyStops, type, request);
  }

  @Override
  public List<Leg> decorateAccessEgressLegs(
    List<Leg> legs,
    WgsCoordinate pickup,
    WgsCoordinate dropoff
  ) {
    return taxiRouter.decorateAccessEgressLegs(legs, pickup, dropoff);
  }

  @Override
  public List<Itinerary> routeDirect(
    TransitService transitService,
    RouteRequest request,
    LinkingContext linkingContext
  ) {
    return taxiRouter.routeDirect(
      graph,
      transitService,
      streetLimitationParametersService,
      vehicleRentalService,
      streetDetailsService,
      dataOverlayParameterBindings,
      request,
      linkingContext
    );
  }
}
