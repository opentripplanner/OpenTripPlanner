package org.opentripplanner.routing.algorithm;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.ext.flex.FlexParameters;
import org.opentripplanner.ext.ridehailing.RideHailingService;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.routing.algorithm.raptoradapter.router.AccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptoradapter.router.AdditionalSearchDays;
import org.opentripplanner.routing.algorithm.raptoradapter.router.CarpoolAccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptoradapter.router.DefaultAccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptoradapter.router.FilterTransitWhenDirectModeIsEmpty;
import org.opentripplanner.routing.algorithm.raptoradapter.router.FlexAccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.AccessEgressMapper;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.transfer.regular.RegularTransferService;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.transit.service.TransitServiceResolver;

/**
 * Decides which routers apply to a given {@link RouteRequest}. This is the single place where
 * all mode-equality, feature-flag, start-on-board, via-search, and transit-enabled checks for
 * router selection live.
 */
class RouterSelector {

  private final RouteRequest request;
  private final TransitService transitService;
  private final Graph graph;
  private final RegularTransferService transferService;
  private final StreetDetailsService streetDetailsService;
  private final FlexParameters flexParameters;
  private final AdditionalSearchDays additionalSearchDays;
  private final List<RideHailingService> rideHailingServices;

  @Nullable
  private final CarpoolingService carpoolingService;

  private final ZonedDateTime transitSearchTimeZero;

  RouterSelector(
    RouteRequest request,
    TransitService transitService,
    Graph graph,
    RegularTransferService transferService,
    StreetDetailsService streetDetailsService,
    FlexParameters flexParameters,
    AdditionalSearchDays additionalSearchDays,
    List<RideHailingService> rideHailingServices,
    @Nullable CarpoolingService carpoolingService,
    ZonedDateTime transitSearchTimeZero
  ) {
    this.request = request;
    this.transitService = transitService;
    this.graph = graph;
    this.transferService = transferService;
    this.streetDetailsService = streetDetailsService;
    this.flexParameters = flexParameters;
    this.additionalSearchDays = additionalSearchDays;
    this.rideHailingServices = rideHailingServices;
    this.carpoolingService = carpoolingService;
    this.transitSearchTimeZero = transitSearchTimeZero;
  }

  /**
   * Selects and returns the list of routers applicable to this request.
   */
  List<Supplier<RoutingResult>> selectRouters(
    BiFunction<RouteRequest, Boolean, RoutingResult> directStreetRunner,
    Supplier<RoutingResult> directFlexRunner,
    Supplier<RoutingResult> directCarpoolRunner,
    Supplier<RoutingResult> transitRunner
  ) {
    List<Supplier<RoutingResult>> routers = new ArrayList<>();
    selectDirectRouters(routers, directStreetRunner, directFlexRunner, directCarpoolRunner);
    selectTransitRouter(routers, transitRunner);
    return routers;
  }

  private void selectDirectRouters(
    List<Supplier<RoutingResult>> routers,
    BiFunction<RouteRequest, Boolean, RoutingResult> directStreetRunner,
    Supplier<RoutingResult> directFlexRunner,
    Supplier<RoutingResult> directCarpoolRunner
  ) {
    // Start-on-board trip locations don't have street vertices, so direct routing doesn't apply.
    if (request.isStartOnBoardAccessRequest()) {
      return;
    }
    StreetMode directMode = request.journey().direct().mode();

    // TODO: Add support for via search to the direct-street search and remove this.
    //       The direct search is used to prune away silly transit results and it
    //       would be nice to also support via as a feature in the direct-street search.
    if (!request.isViaSearch()) {
      // If no direct mode is set, then we set one. See FilterTransitWhenDirectModeIsEmpty.
      var directModeHandler = new FilterTransitWhenDirectModeIsEmpty(
        directMode,
        request.pageCursor() != null
      );
      StreetMode resolvedDirectMode = directModeHandler.resolveDirectMode();
      if (resolvedDirectMode != StreetMode.NOT_SET) {
        var directRequest = request
          .copyOf()
          .withJourney(jb ->
            jb.withDirect(
              new StreetRequest(resolvedDirectMode, request.journey().direct().rentalDuration())
            )
          )
          .buildRequest();
        routers.add(() ->
          directStreetRunner.apply(directRequest, directModeHandler.removeWalkAllTheWayResults())
        );
      }
    }
    if (directMode == StreetMode.FLEXIBLE && OTPFeature.FlexRouting.isOn()) {
      routers.add(directFlexRunner);
    }
    if (directMode == StreetMode.CARPOOL && OTPFeature.CarPooling.isOn()) {
      routers.add(directCarpoolRunner);
    }
  }

  private void selectTransitRouter(
    List<Supplier<RoutingResult>> routers,
    Supplier<RoutingResult> transitRunner
  ) {
    if (request.journey().transit().enabled() && !request.cannotReachTransit()) {
      routers.add(transitRunner);
    }
  }

  /**
   * Selects the {@link AccessEgressRouter}s applicable to the given access/egress
   * {@link StreetMode}.
   */
  List<AccessEgressRouter> selectAccessEgressRouters(StreetMode mode) {
    var transitServiceResolver = new TransitServiceResolver(transitService);
    var accessEgressMapper = new AccessEgressMapper(transitServiceResolver);
    List<AccessEgressRouter> routers = new ArrayList<>();
    routers.add(new DefaultAccessEgressRouter(accessEgressMapper, rideHailingServices, request));
    if (mode == StreetMode.FLEXIBLE && OTPFeature.FlexRouting.isOn()) {
      routers.add(
        new FlexAccessEgressRouter(
          transitService,
          graph,
          transferService,
          streetDetailsService,
          flexParameters,
          additionalSearchDays
        )
      );
    }
    if (mode == StreetMode.CARPOOL && OTPFeature.CarPooling.isOn()) {
      routers.add(
        new CarpoolAccessEgressRouter(
          carpoolingService,
          transitServiceResolver,
          transitSearchTimeZero
        )
      );
    }
    return routers;
  }
}
