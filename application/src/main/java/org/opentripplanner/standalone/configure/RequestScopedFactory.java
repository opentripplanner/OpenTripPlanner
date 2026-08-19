package org.opentripplanner.standalone.configure;

import dagger.Subcomponent;
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.GtfsApiParameters;
import org.opentripplanner.apis.transmodel.TransmodelAPIParameters;
import org.opentripplanner.apis.transmodel.TransmodelGraphQLSchema;
import org.opentripplanner.ext.geocoder.LuceneIndex;
import org.opentripplanner.ext.ojp.parameters.OjpApiParameters;
import org.opentripplanner.ext.ojp.parameters.TriasApiParameters;
import org.opentripplanner.framework.transaction.api.TransactionScope;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.vehicleparking.VehicleParkingService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeService;
import org.opentripplanner.standalone.api.HttpRequestScoped;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.standalone.config.DebugUiConfig;
import org.opentripplanner.standalone.config.routerconfig.VectorTileConfig;
import org.opentripplanner.street.graph.Graph;
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

  TransitAlertService transitAlertService();

  OtpServerRequestContext createServerContext();

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

  @Subcomponent.Builder
  interface Builder {
    RequestScopedFactory build();
  }
}
