package org.opentripplanner.ext.traveltime;

import static javax.imageio.ImageWriteParam.MODE_EXPLICIT;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
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
import java.util.Map;
import javax.media.jai.RasterFactory;
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
import org.opentripplanner.astar.strategy.DurationSkipEdgeStrategy;
import org.opentripplanner.ext.traveltime.geometry.ZSampleGrid;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.api.request.RaptorRequest;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.api.response.RaptorResponse;
import org.opentripplanner.raptor.api.response.StopArrivals;
import org.opentripplanner.raptor.spi.RaptorAccessEgress;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.DefaultAccessEgress;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.AccessEgressMapper;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.RaptorRoutingRequestTransitData;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.RouteRequestTransitDataProviderFilter;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.TemporaryVerticesContainer;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.request.StreetSearchRequestMapper;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateData;
import org.opentripplanner.street.search.strategy.DominanceFunctions;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.TransitService;

@Path("/traveltime")
public class TravelTimeResource {

  private static final SimpleFeatureType contourSchema = makeContourSchema();

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
    @QueryParam("modes") String modes
  ) {
    this.graph = serverContext.graph();
    this.transitService = serverContext.transitService();
    routingRequest = serverContext.defaultRouteRequest();
    routingRequest.setFrom(LocationStringParser.fromOldStyleString(location));
    if (modes != null) {
      routingRequest.journey().setModes(new QualifiedModeSet(modes).getRequestModes());
    }

    traveltimeRequest =
      new TravelTimeRequest(
        cutoffs.stream().map(DurationUtils::duration).toList(),
        routingRequest
          .preferences()
          .street()
          .maxAccessEgressDuration()
          .valueOf(routingRequest.journey().access().mode())
      );

    if (time != null) {
      startTime = Instant.parse(time);
    } else {
      startTime = Instant.now();
    }

    routingRequest.setDateTime(startTime);
    endTime = startTime.plus(traveltimeRequest.maxCutoff);

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
        new RouteRequestTransitDataProviderFilter(routingRequest, transitService),
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
    final RouteRequest accessRequest = routingRequest.clone();

    accessRequest.withPreferences(preferences ->
      preferences.withStreet(it ->
        it.withMaxAccessEgressDuration(traveltimeRequest.maxAccessDuration, Map.of())
      )
    );

    try (
      var temporaryVertices = new TemporaryVerticesContainer(
        graph,
        accessRequest,
        accessRequest.journey().access().mode(),
        StreetMode.NOT_SET
      )
    ) {
      final Collection<DefaultAccessEgress> accessList = getAccess(
        accessRequest,
        temporaryVertices
      );

      var arrivals = route(accessList).getArrivals();

      var spt = StreetSearchBuilder
        .of()
        .setSkipEdgeStrategy(new DurationSkipEdgeStrategy(traveltimeRequest.maxCutoff))
        .setRequest(routingRequest)
        .setStreetRequest(accessRequest.journey().access())
        .setVerticesContainer(temporaryVertices)
        .setDominanceFunction(new DominanceFunctions.EarliestArrival())
        .setInitialStates(getInitialStates(arrivals, temporaryVertices))
        .getShortestPathTree();

      return SampleGridRenderer.getSampleGrid(spt, traveltimeRequest);
    }
  }

  private Collection<DefaultAccessEgress> getAccess(
    RouteRequest accessRequest,
    TemporaryVerticesContainer temporaryVertices
  ) {
    final Collection<NearbyStop> accessStops = AccessEgressRouter.streetSearch(
      accessRequest,
      temporaryVertices,
      transitService,
      routingRequest.journey().access(),
      null,
      false,
      traveltimeRequest.maxAccessDuration
    );
    return new AccessEgressMapper().mapNearbyStops(accessStops, false);
  }

  private List<State> getInitialStates(
    StopArrivals arrivals,
    TemporaryVerticesContainer temporaryVertices
  ) {
    List<State> initialStates = new ArrayList<>();

    StreetSearchRequest streetSearchRequest = StreetSearchRequestMapper
      .map(routingRequest)
      .withMode(routingRequest.journey().egress().mode())
      .withArriveBy(false)
      .build();

    StateData stateData = StateData.getInitialStateData(streetSearchRequest);

    for (var vertex : temporaryVertices.getFromVertices()) {
      // TODO StateData should be of direct mode here
      initialStates.add(new State(vertex, startTime, stateData, streetSearchRequest));
    }

    // TODO - Add a method to return all Stops, not StopLocations
    for (RegularStop stop : transitService.listRegularStops()) {
      int index = stop.getIndex();
      if (arrivals.reachedByTransit(index)) {
        final int arrivalTime = arrivals.bestTransitArrivalTime(index);
        Vertex v = graph.getStopVertexForStopId(stop.getId());
        if (v != null) {
          Instant time = startOfTime.plusSeconds(arrivalTime).toInstant();
          State s = new State(v, time, stateData.clone(), streetSearchRequest);
          s.weight = startTime.until(time, ChronoUnit.SECONDS);
          initialStates.add(s);
        }
      }
    }
    return initialStates;
  }

  private RaptorResponse<TripSchedule> route(Collection<? extends RaptorAccessEgress> accessList) {
    final RaptorRequest<TripSchedule> request = new RaptorRequestBuilder<TripSchedule>()
      .profile(RaptorProfile.BEST_TIME)
      .searchParams()
      .earliestDepartureTime(ServiceDateUtils.secondsSinceStartOfTime(startOfTime, startTime))
      .latestArrivalTime(ServiceDateUtils.secondsSinceStartOfTime(startOfTime, endTime))
      .addAccessPaths(accessList)
      .searchOneIterationOnly()
      .timetableEnabled(false)
      .allowEmptyEgressPaths(true)
      .constrainedTransfersEnabled(false) // TODO: Not compatible with best times
      .build();

    return raptorService.route(request, requestTransitDataProvider);
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
