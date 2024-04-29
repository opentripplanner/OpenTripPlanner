package org.opentripplanner.ext.traveltime;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.geotools.data.geojson.GeoJSONWriter;
import org.opentripplanner.api.common.LocationStringParser;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.ext.traveltime.geometry.ZSampleGrid;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.api.response.RaptorResponse;
import org.opentripplanner.raptor.api.response.StopArrivals;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.DefaultAccessEgress;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.AccessEgressMapper;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.RaptorRoutingRequestTransitData;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.RouteRequestTransitDataProviderFilter;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.api.request.request.filter.SelectRequest;
import org.opentripplanner.routing.api.request.request.filter.TransitFilterRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.TemporaryVerticesContainer;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.request.StreetSearchRequestMapper;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateData;
import org.opentripplanner.street.search.strategy.DominanceFunctions;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.TransitService;

@Path("/traveltime")
public class TravelTimeResource {

  private final RouteRequest routingRequest;
  private final RaptorRoutingRequestTransitData requestTransitDataProvider;
  private final Instant startTime;
  private final Instant endTime;
  private final ZonedDateTime startOfTime;
  private final TravelTimeRequest traveltimeRequest;
  private final RaptorService<TripSchedule> raptorService;
  private final Graph graph;
  private final TransitService transitService;

  public TravelTimeResource(
    @Context OtpServerRequestContext serverContext,
    @QueryParam("location") String location,
    @QueryParam("time") String time,
    @QueryParam("cutoff") @DefaultValue("60m") List<String> cutoffs,
    @QueryParam("modes") String modes,
    @QueryParam("arriveBy") @DefaultValue("false") boolean arriveBy
  ) {
    this.graph = serverContext.graph();
    this.transitService = serverContext.transitService();
    routingRequest = serverContext.defaultRouteRequest();
    routingRequest.setArriveBy(arriveBy);

    if (modes != null) {
      var modeSet = new QualifiedModeSet(modes);
      routingRequest.journey().setModes(modeSet.getRequestModes());
      var transitModes = modeSet.getTransitModes().stream().map(MainAndSubMode::new).toList();
      var select = SelectRequest.of().withTransportModes(transitModes).build();
      var request = TransitFilterRequest.of().addSelect(select).build();
      routingRequest.journey().transit().setFilters(List.of(request));
    }

    var durationForMode = routingRequest.preferences().street().accessEgress().maxDuration();
    traveltimeRequest =
      new TravelTimeRequest(
        cutoffs.stream().map(DurationUtils::duration).toList(),
        durationForMode.valueOf(getAccessRequest(routingRequest).mode()),
        durationForMode.valueOf(getEgressRequest(routingRequest).mode())
      );

    var parsedLocation = LocationStringParser.fromOldStyleString(location);
    var requestTime = time != null ? Instant.parse(time) : Instant.now();
    routingRequest.setDateTime(requestTime);

    if (routingRequest.arriveBy()) {
      startTime = requestTime.minus(traveltimeRequest.maxCutoff);
      endTime = requestTime;
      routingRequest.setTo(parsedLocation);
    } else {
      startTime = requestTime;
      endTime = startTime.plus(traveltimeRequest.maxCutoff);
      routingRequest.setFrom(parsedLocation);
    }

    ZoneId zoneId = transitService.getTimeZone();
    LocalDate startDate = LocalDate.ofInstant(startTime, zoneId);
    LocalDate endDate = LocalDate.ofInstant(endTime, zoneId);
    startOfTime = ServiceDateUtils.asStartOfService(startDate, zoneId);

    requestTransitDataProvider =
      new RaptorRoutingRequestTransitData(
        transitService.getRealtimeTransitLayer(),
        startOfTime,
        0,
        (int) Period.between(startDate, endDate).get(ChronoUnit.DAYS),
        new RouteRequestTransitDataProviderFilter(routingRequest),
        routingRequest
      );

    raptorService = new RaptorService<>(serverContext.raptorConfig());
  }

  @GET
  @Path("/isochrone")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getIsochrones() {
    ZSampleGrid<WTWD> sampleGrid = getSampleGrid();

    var isochrones = IsochroneRenderer.renderIsochrones(sampleGrid, traveltimeRequest);
    var features = IsochroneRenderer.makeContourFeatures(isochrones);

    StreamingOutput out = outputStream -> {
      try (final GeoJSONWriter geoJSONWriter = new GeoJSONWriter(outputStream)) {
        geoJSONWriter.writeFeatureCollection(features);
      }
    };

    return Response.ok().entity(out).build();
  }

  @GET
  @Path("/surface")
  @Produces("image/tiff")
  public Response getSurface() {
    ZSampleGrid<WTWD> sampleGrid = getSampleGrid();
    StreamingOutput streamingOutput = RasterRenderer.createGeoTiffRaster(sampleGrid);
    return Response.ok().entity(streamingOutput).build();
  }

  private ZSampleGrid<WTWD> getSampleGrid() {
    try (
      var temporaryVertices = new TemporaryVerticesContainer(
        graph,
        routingRequest,
        getAccessRequest(routingRequest).mode(),
        StreetMode.NOT_SET
      )
    ) {
      var accessList = getAccess(temporaryVertices);
      var arrivals = route(accessList).getArrivals();
      var spt = getShortestPathTree(temporaryVertices, arrivals);
      return SampleGridRenderer.getSampleGrid(spt, traveltimeRequest);
    }
  }

  private Collection<DefaultAccessEgress> getAccess(TemporaryVerticesContainer temporaryVertices) {
    final Collection<NearbyStop> accessStops = AccessEgressRouter.streetSearch(
      routingRequest,
      temporaryVertices,
      transitService,
      getAccessRequest(routingRequest),
      null,
      routingRequest.arriveBy(),
      traveltimeRequest.maxAccessDuration,
      0
    );
    return AccessEgressMapper.mapNearbyStops(accessStops, routingRequest.arriveBy());
  }

  private ShortestPathTree<State, Edge, Vertex> getShortestPathTree(
    TemporaryVerticesContainer temporaryVertices,
    StopArrivals arrivals
  ) {
    return StreetSearchBuilder
      .of()
      .setSkipEdgeStrategy(
        new PostTransitSkipEdgeStrategy(
          traveltimeRequest.maxEgressDuration,
          routingRequest.dateTime(),
          routingRequest.arriveBy()
        )
      )
      .setRequest(routingRequest)
      .setStreetRequest(getEgressRequest(routingRequest))
      .setVerticesContainer(temporaryVertices)
      .setDominanceFunction(new DominanceFunctions.EarliestArrival())
      .setInitialStates(getInitialStates(arrivals, temporaryVertices))
      .getShortestPathTree();
  }

  private List<State> getInitialStates(
    StopArrivals arrivals,
    TemporaryVerticesContainer temporaryVertices
  ) {
    List<State> initialStates = new ArrayList<>();

    StreetSearchRequest directStreetSearchRequest = StreetSearchRequestMapper
      .map(routingRequest)
      .withMode(routingRequest.journey().direct().mode())
      .withArriveBy(routingRequest.arriveBy())
      .build();

    List<StateData> directStateDatas = StateData.getInitialStateDatas(directStreetSearchRequest);

    Set<Vertex> vertices = routingRequest.arriveBy()
      ? temporaryVertices.getToVertices()
      : temporaryVertices.getFromVertices();
    for (var vertex : vertices) {
      for (var stateData : directStateDatas) {
        initialStates.add(new State(vertex, startTime, stateData, directStreetSearchRequest));
      }
    }

    StreetSearchRequest egressStreetSearchRequest = StreetSearchRequestMapper
      .map(routingRequest)
      .withMode(getEgressRequest(routingRequest).mode())
      .withArriveBy(routingRequest.arriveBy())
      .build();

    for (RegularStop stop : transitService.listRegularStops()) {
      int index = stop.getIndex();
      if (!arrivals.reachedByTransit(index)) {
        continue;
      }
      final int arrivalTime = arrivals.bestTransitArrivalTime(index);
      Vertex v = graph.getStopVertexForStopId(stop.getId());
      if (v == null) {
        continue;
      }
      Instant time = startOfTime.plusSeconds(arrivalTime).toInstant();
      List<StateData> egressStateDatas = StateData.getInitialStateDatas(
        egressStreetSearchRequest,
        mode -> new TravelTimeStateData(mode, time.getEpochSecond())
      );
      for (var stopStateData : egressStateDatas) {
        State s = new State(v, time, stopStateData, directStreetSearchRequest);
        s.weight =
          routingRequest.arriveBy()
            ? time.until(endTime, ChronoUnit.SECONDS)
            : startTime.until(time, ChronoUnit.SECONDS);
        initialStates.add(s);
      }
    }
    return initialStates;
  }

  private RaptorResponse<TripSchedule> route(Collection<? extends RaptorAccessEgress> accessList) {
    RaptorRequestBuilder<TripSchedule> builder = new RaptorRequestBuilder<>();

    builder
      .profile(RaptorProfile.BEST_TIME)
      .searchParams()
      .earliestDepartureTime(ServiceDateUtils.secondsSinceStartOfTime(startOfTime, startTime))
      .latestArrivalTime(ServiceDateUtils.secondsSinceStartOfTime(startOfTime, endTime))
      .searchOneIterationOnly()
      .timetable(false)
      .allowEmptyAccessEgressPaths(true)
      .constrainedTransfers(false); // TODO: Not compatible with best times

    if (routingRequest.arriveBy()) {
      builder.searchDirection(SearchDirection.REVERSE).searchParams().addEgressPaths(accessList);
    } else {
      builder.searchDirection(SearchDirection.FORWARD).searchParams().addAccessPaths(accessList);
    }

    return raptorService.route(builder.build(), requestTransitDataProvider);
  }

  private StreetRequest getAccessRequest(RouteRequest accessRequest) {
    return routingRequest.arriveBy()
      ? accessRequest.journey().egress()
      : accessRequest.journey().access();
  }

  private StreetRequest getEgressRequest(RouteRequest accessRequest) {
    return routingRequest.arriveBy()
      ? accessRequest.journey().access()
      : accessRequest.journey().egress();
  }
}
