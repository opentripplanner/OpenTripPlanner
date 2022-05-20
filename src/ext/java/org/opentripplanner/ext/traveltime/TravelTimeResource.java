package org.opentripplanner.ext.traveltime;

import static javax.imageio.ImageWriteParam.MODE_EXPLICIT;

import java.awt.image.DataBuffer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.media.jai.RasterFactory;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.geojson.MultiPolygon;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.data.geojson.GeoJSONWriter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opentripplanner.api.common.LocationStringParser;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.ext.traveltime.geometry.ZSampleGrid;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.routing.algorithm.astar.AStarBuilder;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.AccessEgress;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.Transfer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.AccessEgressMapper;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.DateMapper;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.RaptorRoutingRequestTransitData;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.RoutingRequestTransitDataProviderFilter;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateData;
import org.opentripplanner.routing.core.TemporaryVerticesContainer;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.standalone.server.OTPServer;
import org.opentripplanner.standalone.server.Router;
import org.opentripplanner.transit.raptor.api.request.RaptorProfile;
import org.opentripplanner.transit.raptor.api.request.RaptorRequest;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.view.Worker;
import org.opentripplanner.transit.raptor.rangeraptor.RangeRaptorWorker;
import org.opentripplanner.transit.raptor.rangeraptor.standard.ArrivalTimeRoutingStrategy;
import org.opentripplanner.transit.raptor.rangeraptor.standard.StdRangeRaptorWorkerState;
import org.opentripplanner.transit.raptor.rangeraptor.standard.besttimes.BestTimes;
import org.opentripplanner.transit.raptor.rangeraptor.standard.besttimes.BestTimesOnlyStopArrivalsState;
import org.opentripplanner.transit.raptor.rangeraptor.standard.besttimes.SimpleBestNumberOfTransfers;
import org.opentripplanner.transit.raptor.rangeraptor.transit.SearchContext;
import org.opentripplanner.util.time.DurationUtils;

@Path("/traveltime")
public class TravelTimeResource {

  private static final SimpleFeatureType contourSchema = makeContourSchema();

  private final Router router;
  private final RoutingRequest routingRequest;
  private final TransitLayer transitLayer;
  private final RaptorRoutingRequestTransitData requestTransitDataProvider;
  private final Instant startTime;
  private final Instant endTime;
  private final ZonedDateTime startOfTime;
  private final TravelTimeRequest traveltimeRequest;
  private final long start;
  private BestTimes bestTimes;
  private SearchContext<TripSchedule> raptorContext;

  public TravelTimeResource(
    @Context OTPServer otpServer,
    @QueryParam("location") String location,
    @QueryParam("time") String time,
    @QueryParam("cutoff") @DefaultValue("60m") List<String> cutoffs,
    @QueryParam("modes") String modes
  ) {
    start = System.currentTimeMillis();
    router = otpServer.getRouter();
    transitLayer = router.graph.getRealtimeTransitLayer();
    ZoneId zoneId = transitLayer.getTransitDataZoneId();
    routingRequest = router.copyDefaultRoutingRequest();
    routingRequest.from = LocationStringParser.fromOldStyleString(location);
    if (modes != null) {
      routingRequest.modes = new QualifiedModeSet(modes).getRequestModes();
    }
    traveltimeRequest =
      new TravelTimeRequest(
        cutoffs.stream().map(DurationUtils::duration).toList(),
        routingRequest.getMaxAccessEgressDuration(routingRequest.modes.accessMode)
      );

    if (time != null) {
      startTime = Instant.parse(time);
    } else {
      startTime = Instant.now();
    }

    endTime = startTime.plus(traveltimeRequest.maxCutoff);

    LocalDate startDate = LocalDate.ofInstant(startTime, zoneId);
    LocalDate endDate = LocalDate.ofInstant(endTime, zoneId);
    startOfTime = startDate.atStartOfDay(zoneId).toInstant().atZone(zoneId);

    RoutingRequest transferRoutingRequest = Transfer.prepareTransferRoutingRequest(routingRequest);

    requestTransitDataProvider =
      new RaptorRoutingRequestTransitData(
        router.graph.getTransferService(),
        transitLayer,
        startOfTime,
        0,
        (int) Period.between(startDate, endDate).get(ChronoUnit.DAYS),
        new RoutingRequestTransitDataProviderFilter(routingRequest, router.graph.index),
        new RoutingContext(transferRoutingRequest, router.graph, (Vertex) null, null)
      );
  }

  @GET
  @Path("/isochrone")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getIsochrones() {
    ZSampleGrid<WTWD> sampleGrid = getSampleGrid();

    var isochrones = IsochroneRenderer.renderIsochrones(sampleGrid, traveltimeRequest);

    var features = makeContourFeatures(isochrones);

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

    int minX = sampleGrid.getXMin();
    int minY = sampleGrid.getYMin();
    int maxY = sampleGrid.getYMax();

    int width = sampleGrid.getXMax() - minX + 1;
    int height = maxY - minY + 1;

    Coordinate center = sampleGrid.getCenter();

    double resX = sampleGrid.getCellSize().x;
    double resY = sampleGrid.getCellSize().y;

    var raster = RasterFactory.createBandedRaster(DataBuffer.TYPE_INT, width, height, 1, null);
    var dataBuffer = raster.getDataBuffer();

    // Initialize with NO DATA value
    for (int i = 0; i < dataBuffer.getSize(); i++) {
      dataBuffer.setElem(i, Integer.MIN_VALUE);
    }

    for (var s : sampleGrid) {
      final WTWD z = s.getZ();
      raster.setSample(s.getX() - minX, maxY - s.getY(), 0, z.wTime / z.w);
    }

    Envelope2D geom = new GridGeometry2D(
      new GridEnvelope2D(0, 0, width, height),
      new AffineTransform2D(resX, 0, 0, resY, center.x + resX * minX, center.y + resY * minY),
      DefaultGeographicCRS.WGS84
    )
      .getEnvelope2D();

    GridCoverage2D gridCoverage = new GridCoverageFactory().create("traveltime", raster, geom);

    GeoTiffWriteParams wp = new GeoTiffWriteParams();
    wp.setCompressionMode(MODE_EXPLICIT);
    wp.setCompressionType("LZW");
    ParameterValueGroup params = new GeoTiffFormat().getWriteParameters();
    params.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString()).setValue(wp);
    StreamingOutput streamingOutput = outputStream -> {
      GeoTiffWriter writer = new GeoTiffWriter(outputStream);
      writer.write(gridCoverage, params.values().toArray(new GeneralParameterValue[1]));
      writer.dispose();
      outputStream.close();
    };
    return Response.ok().entity(streamingOutput).build();
  }

  private ZSampleGrid<WTWD> getSampleGrid() {
    long preAccess = System.currentTimeMillis();

    final RoutingRequest accessRequest = routingRequest.clone();

    accessRequest.maxAccessEgressDuration = traveltimeRequest.maxAccessDuration;

    try (var temporaryVertices = new TemporaryVerticesContainer(router.graph, accessRequest)) {
      final Collection<AccessEgress> accessList = getAccess(accessRequest, temporaryVertices);

      final long postAccess = System.currentTimeMillis();

      getRaptorWorker(accessList).route();

      final long postRaptor = System.currentTimeMillis();

      RoutingContext routingContext = new RoutingContext(
        routingRequest,
        router.graph,
        temporaryVertices
      );

      var spt = AStarBuilder
        .allDirectionsMaxDuration(traveltimeRequest.maxCutoff)
        .setContext(routingContext)
        .setDominanceFunction(new DominanceFunction.EarliestArrival())
        .setInitialStates(getInitialStates(temporaryVertices, routingContext))
        .getShortestPathTree();

      return SampleGridRenderer.getSampleGrid(spt, traveltimeRequest);
    }
  }

  private Collection<AccessEgress> getAccess(
    RoutingRequest accessRequest,
    TemporaryVerticesContainer temporaryVertices
  ) {
    final Collection<NearbyStop> accessStops = AccessEgressRouter.streetSearch(
      new RoutingContext(accessRequest, router.graph, temporaryVertices),
      routingRequest.modes.accessMode,
      false
    );
    return new AccessEgressMapper(transitLayer.getStopIndex()).mapNearbyStops(accessStops, false);
  }

  private List<State> getInitialStates(
    TemporaryVerticesContainer temporaryVertices,
    RoutingContext routingContext
  ) {
    List<State> initialStates = new ArrayList<>();

    StateData stateData = StateData.getInitialStateData(routingRequest);

    for (var vertex : temporaryVertices.getFromVertices()) {
      initialStates.add(new State(vertex, startTime, routingContext, stateData));
    }

    final int unreachedTime = raptorContext.calculator().unreachedTime();

    for (int i = 0; i < transitLayer.getStopIndex().size(); i++) {
      final int onBoardTime = bestTimes.onBoardTime(i);
      if (onBoardTime != unreachedTime) {
        StopLocation stopLocation = transitLayer.getStopIndex().stopByIndex(i);
        if (stopLocation instanceof Stop stop) {
          Vertex v = router.graph.index.getStopVertexForStop().get(stop);
          if (v != null) {
            Instant time = startOfTime.plusSeconds(onBoardTime).toInstant();
            State s = new State(v, time, routingContext, stateData.clone());
            s.weight = startTime.until(time, ChronoUnit.SECONDS);
            // TODO: This shouldn't be overridden in state initialization
            s.stateData.startTime = stateData.startTime;
            initialStates.add(s);
          }
        }
      }
    }
    return initialStates;
  }

  private Worker<TripSchedule> getRaptorWorker(Collection<? extends RaptorTransfer> accessList) {
    final RaptorRequest<TripSchedule> request = new RaptorRequestBuilder<TripSchedule>()
      .profile(RaptorProfile.BEST_TIME)
      .searchParams()
      .earliestDepartureTime(DateMapper.secondsSinceStartOfTime(startOfTime, startTime))
      .latestArrivalTime(DateMapper.secondsSinceStartOfTime(startOfTime, endTime))
      .addAccessPaths(accessList)
      .searchOneIterationOnly()
      .timetableEnabled(false)
      .allowEmptyEgressPaths(true)
      .constrainedTransfersEnabled(false) // TODO: Not compatible with best times
      .build();

    raptorContext = router.raptorConfig.context(requestTransitDataProvider, request);

    bestTimes =
      new BestTimes(raptorContext.nStops(), raptorContext.calculator(), raptorContext.lifeCycle());

    final SimpleBestNumberOfTransfers simpleBestNumberOfTransfers = new SimpleBestNumberOfTransfers(
      raptorContext.nStops(),
      raptorContext.roundProvider()
    );

    final BestTimesOnlyStopArrivalsState<TripSchedule> stopArrivalsState = new BestTimesOnlyStopArrivalsState<>(
      bestTimes,
      simpleBestNumberOfTransfers
    );

    final StdRangeRaptorWorkerState<TripSchedule> workerState = new StdRangeRaptorWorkerState<>(
      raptorContext.calculator(),
      bestTimes,
      stopArrivalsState,
      () -> false
    );

    final ArrivalTimeRoutingStrategy<TripSchedule> transitWorker = new ArrivalTimeRoutingStrategy<>(
      raptorContext.calculator(),
      workerState
    );

    return new RangeRaptorWorker<>(
      workerState,
      transitWorker,
      raptorContext.transit(),
      raptorContext.slackProvider(),
      raptorContext.accessPaths(),
      raptorContext.roundProvider(),
      raptorContext.calculator(),
      raptorContext.createLifeCyclePublisher(),
      raptorContext.timers(),
      raptorContext.enableConstrainedTransfers()
    );
  }

  static SimpleFeatureType makeContourSchema() {
    /* Create the output feature schema. */
    SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
    typeBuilder.setName("contours");
    typeBuilder.setCRS(DefaultGeographicCRS.WGS84);
    typeBuilder.setDefaultGeometry("the_geom");
    // Do not use "geom" or "geometry" below, it seems to broke shapefile generation
    typeBuilder.add("the_geom", MultiPolygon.class);
    typeBuilder.add("time", Long.class);
    return typeBuilder.buildFeatureType();
  }

  /**
   * Create a geotools feature collection from a list of isochrones in the OTPA internal format.
   * Once in a FeatureCollection, they can for example be exported as GeoJSON.
   */
  private static SimpleFeatureCollection makeContourFeatures(List<IsochroneData> isochrones) {
    DefaultFeatureCollection featureCollection = new DefaultFeatureCollection(null, contourSchema);
    SimpleFeatureBuilder fbuilder = new SimpleFeatureBuilder(contourSchema);
    for (IsochroneData isochrone : isochrones) {
      fbuilder.add(isochrone.geometry());
      fbuilder.add(isochrone.cutoffSec());
      featureCollection.add(fbuilder.buildFeature(null));
    }
    return featureCollection;
  }
}
