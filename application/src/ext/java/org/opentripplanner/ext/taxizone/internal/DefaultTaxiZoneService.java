package org.opentripplanner.ext.taxizone.internal;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.ext.dataoverlay.configuration.DataOverlayParameterBindings;
import org.opentripplanner.ext.taxizone.TaxiZoneIndex;
import org.opentripplanner.ext.taxizone.TaxiZoneService;
import org.opentripplanner.ext.taxizone.model.TaxiZone;
import org.opentripplanner.ext.taxizone.routing.TaxiRouter;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transit.service.TransitService;

public class DefaultTaxiZoneService implements TaxiZoneService {

  private final TaxiRouter taxiRouter;
  private final Graph graph;
  private final StreetLimitationParametersService streetLimitationParametersService;
  private final VehicleRentalService vehicleRentalService;
  private final StreetDetailsService streetDetailsService;

  @Nullable
  private final DataOverlayParameterBindings dataOverlayParameterBindings;

  public DefaultTaxiZoneService(
    List<TaxiZone> zones,
    Graph graph,
    StreetLimitationParametersService streetLimitationParametersService,
    VehicleRentalService vehicleRentalService,
    StreetDetailsService streetDetailsService,
    @Nullable DataOverlayParameterBindings dataOverlayParameterBindings
  ) {
    this.taxiRouter = new TaxiRouter(new TaxiZoneIndex(zones));
    this.graph = Objects.requireNonNull(graph);
    this.streetLimitationParametersService = Objects.requireNonNull(
      streetLimitationParametersService
    );
    this.vehicleRentalService = Objects.requireNonNull(vehicleRentalService);
    this.streetDetailsService = Objects.requireNonNull(streetDetailsService);
    this.dataOverlayParameterBindings = dataOverlayParameterBindings;
  }

  @Override
  public List<Itinerary> decorateAndFilter(List<Itinerary> itineraries) {
    return taxiRouter.decorateAndFilter(itineraries);
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
