package org.opentripplanner.standalone.configure;

import dagger.Subcomponent;
import graphql.schema.GraphQLSchema;
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.GtfsApiParameters;
import org.opentripplanner.apis.gtfs.GtfsGraphQLRequestContext;
import org.opentripplanner.apis.gtfs.configure.GtfsSchema;
import org.opentripplanner.apis.transmodel.TransmodelAPIParameters;
import org.opentripplanner.apis.transmodel.TransmodelGraphQLRequestContext;
import org.opentripplanner.apis.transmodel.TransmodelGraphQLSchema;
import org.opentripplanner.ext.empiricaldelay.EmpiricalDelayService;
import org.opentripplanner.ext.geocoder.LuceneIndex;
import org.opentripplanner.ext.ojp.parameters.OjpApiParameters;
import org.opentripplanner.ext.ojp.parameters.TriasApiParameters;
import org.opentripplanner.framework.transaction.api.TransactionScope;
import org.opentripplanner.routing.api.RoutingService;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleService;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.vehicleparking.VehicleParkingService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeService;
import org.opentripplanner.standalone.api.HttpRequestScoped;
import org.opentripplanner.standalone.config.DebugUiConfig;
import org.opentripplanner.standalone.config.routerconfig.VectorTileConfig;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transfer.regular.RegularTransferService;
import org.opentripplanner.transit.service.TransitService;

/**
 * A Dagger subcomponent scoped to the lifetime of one HTTP request. Every binding here is derived
 * from a single {@link TransactionScope} captured once per {@link Builder#build()}, so they are
 * guaranteed to be consistent with each other — no possibility of a mid-request update being
 * visible to one binding but not another.
 * <p>
 * Build one instance per actual HTTP request (never reuse across requests, never share across
 * concurrent requests) — see issue #7441.
 */
@HttpRequestScoped
@Subcomponent(modules = { RequestScopedModule.class })
public interface RequestScopedFactory {
  TransactionScope transactionScope();

  TransitService transitService();

  Graph graph();

  RouteRequest defaultRouteRequest();

  VehicleParkingService vehicleParkingService();

  @Nullable
  LuceneIndex luceneIndex();

  DebugUiConfig debugUiConfig();

  WorldEnvelopeService worldEnvelopeService();

  VehicleRentalService vehicleRentalService();

  StreetDetailsService streetDetailsService();

  RegularTransferService transferService();

  VectorTileConfig vectorTileConfig();

  GtfsApiParameters gtfsApiParameters();

  TransmodelAPIParameters transmodelAPIParameters();

  TransmodelGraphQLSchema transmodelGraphQLSchema();

  LinkingContextFactory linkingContextFactory();

  OjpApiParameters ojpApiParameters();

  TriasApiParameters triasApiParameters();

  RoutingService routingService();

  TransitAlertService transitAlertService();

  FareService fareService();

  RealtimeVehicleService realtimeVehicleService();

  @Nullable
  @GtfsSchema
  GraphQLSchema gtfsSchema();

  StreetLimitationParametersService streetLimitationParametersService();

  @Nullable
  EmpiricalDelayService empiricalDelayService();

  TransmodelGraphQLRequestContext transmodelRequestContext();

  GtfsGraphQLRequestContext graphQLRequestContext();

  @Subcomponent.Builder
  interface Builder {
    RequestScopedFactory build();
  }
}
