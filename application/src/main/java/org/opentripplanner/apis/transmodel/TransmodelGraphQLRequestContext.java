package org.opentripplanner.apis.transmodel;

import javax.annotation.Nullable;
import org.opentripplanner.ext.empiricaldelay.EmpiricalDelayService;
import org.opentripplanner.place.NearbyPlaceFinder;
import org.opentripplanner.place.NearbyStopFinder;
import org.opentripplanner.routing.api.RoutingService;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.vehicleparking.VehicleParkingService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transfer.regular.RegularTransferService;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.lang.Sandbox;

/**
 * The per-HTTP-request context threaded through the Transmodel GraphQL API's data fetchers,
 * bundling the services a query may need (routing, transit, empirical delay, ...).
 * <p>
 * Implementations should resolve these dependencies lazily rather than up front: the production
 * implementation delegates to the request-scoped Dagger component, so a service is only
 * constructed if some data fetcher actually asks for it during that request. This interface also
 * lets tests substitute a plain, hand-built implementation (e.g. {@code
 * TestTransmodelGraphQLRequestContext}) without needing a Dagger component at all.
 */
public interface TransmodelGraphQLRequestContext {
  RoutingService getRoutingService();

  TransitService getTransitService();

  TransitAlertService getTransitAlertService();

  @Nullable
  @Sandbox
  EmpiricalDelayService getEmpiricalDelayService();

  RouteRequest getDefaultRouteRequest();

  VehicleRentalService getVehicleRentalService();

  VehicleParkingService getVehicleParkingService();

  Graph getGraph();

  RegularTransferService getTransferService();

  StreetDetailsService getStreetDetailsService();

  LinkingContextFactory getLinkingContextFactory();

  StreetLimitationParametersService getStreetLimitationParametersService();

  NearbyPlaceFinder getNearbyPlaceFinder();

  NearbyStopFinder getNearbyStopFinder();
}
