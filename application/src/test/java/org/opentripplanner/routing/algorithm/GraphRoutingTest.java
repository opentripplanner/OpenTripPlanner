package org.opentripplanner.routing.algorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehicleparking.model.VehicleParking.VehicleParkingEntranceCreator;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingHelper;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.service.vehiclerental.street.StreetVehicleRentalLink;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalEdge;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.ElevatorAlightEdge;
import org.opentripplanner.street.model.edge.ElevatorBoardEdge;
import org.opentripplanner.street.model.edge.ElevatorEdge;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;
import org.opentripplanner.street.model.edge.FreeEdge;
import org.opentripplanner.street.model.edge.PathwayEdge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;
import org.opentripplanner.street.model.edge.StreetStationCentroidLink;
import org.opentripplanner.street.model.edge.StreetTransitEntranceLink;
import org.opentripplanner.street.model.edge.StreetTransitStopLink;
import org.opentripplanner.street.model.edge.StreetVehicleParkingLink;
import org.opentripplanner.street.model.edge.TemporaryFreeEdge;
import org.opentripplanner.street.model.vertex.ElevatorOnboardVertex;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.StationCentroidVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.TemporaryVertex;
import org.opentripplanner.street.model.vertex.TransitEntranceVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.model.vertex.VertexFactory;
import org.opentripplanner.street.model.vertex.VertexLabel;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.Entrance;
import org.opentripplanner.transit.model.site.PathwayMode;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StationBuilder;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TimetableRepository;

public abstract class GraphRoutingTest {

  public static final String TEST_VEHICLE_RENTAL_NETWORK = "test network";

  protected TestOtpModel modelOf(Builder builder) {
    builder.build();
    Graph graph = builder.graph();
    TimetableRepository timetableRepository = builder.timetableRepository();
    return new TestOtpModel(graph, timetableRepository).index();
  }

  public abstract static class Builder {

    private final Graph graph;
    private final TimetableRepository timetableRepository;
    private final VertexFactory vertexFactory;
    private final VehicleParkingHelper vehicleParkingHelper;

    protected Builder() {
      var deduplicator = new Deduplicator();
      graph = new Graph(deduplicator);
      timetableRepository = new TimetableRepository(new SiteRepository(), deduplicator);
      vertexFactory = new VertexFactory(graph);
      vehicleParkingHelper = new VehicleParkingHelper(graph);
    }

    public abstract void build();

    public Graph graph() {
      return graph;
    }

    public TimetableRepository timetableRepository() {
      return timetableRepository;
    }

    public <T extends Vertex> T v(VertexLabel label) {
      return vertex(label);
    }

    public <T extends Vertex> T vertex(VertexLabel label) {
      return (T) graph.getVertex(label);
    }

    // -- Street network
    public IntersectionVertex intersection(String label, double latitude, double longitude) {
      return vertexFactory.intersection(label, longitude, latitude);
    }

    public IntersectionVertex intersection(String label, WgsCoordinate coordinate) {
      return intersection(label, coordinate.latitude(), coordinate.longitude());
    }

    public StreetEdgeBuilder<?> streetBuilder(
      StreetVertex from,
      StreetVertex to,
      int length,
      StreetTraversalPermission permissions
    ) {
      return new StreetEdgeBuilder<>()
        .withFromVertex(from)
        .withToVertex(to)
        .withGeometry(
          GeometryUtils.makeLineString(from.getLat(), from.getLon(), to.getLat(), to.getLon())
        )
        .withName(String.format("%s%s street", from.getLabel(), to.getLabel()))
        .withMeterLength(length)
        .withPermission(permissions)
        .withBack(false);
    }

    /**
     * Create a street with all permissions in both directions
     */
    public List<StreetEdge> biStreet(StreetVertex from, StreetVertex to, int length) {
      return street(from, to, length, StreetTraversalPermission.ALL, StreetTraversalPermission.ALL);
    }

    public StreetEdge street(
      StreetVertex from,
      StreetVertex to,
      int length,
      StreetTraversalPermission permissions
    ) {
      return streetBuilder(from, to, length, permissions).buildAndConnect();
    }

    public List<StreetEdge> street(
      StreetVertex from,
      StreetVertex to,
      int length,
      StreetTraversalPermission forwardPermissions,
      StreetTraversalPermission reversePermissions
    ) {
      return List.of(
        new StreetEdgeBuilder<>()
          .withFromVertex(from)
          .withToVertex(to)
          .withGeometry(
            GeometryUtils.makeLineString(from.getLat(), from.getLon(), to.getLat(), to.getLon())
          )
          .withName(String.format("%s%s street", from.getDefaultName(), to.getDefaultName()))
          .withMeterLength(length)
          .withPermission(forwardPermissions)
          .withBack(false)
          .buildAndConnect(),
        new StreetEdgeBuilder<>()
          .withFromVertex(to)
          .withToVertex(from)
          .withGeometry(
            GeometryUtils.makeLineString(to.getLat(), to.getLon(), from.getLat(), from.getLon())
          )
          .withName(String.format("%s%s street", from.getDefaultName(), to.getDefaultName()))
          .withMeterLength(length)
          .withPermission(reversePermissions)
          .withBack(true)
          .buildAndConnect()
      );
    }

    public List<ElevatorEdge> elevator(StreetTraversalPermission permission, Vertex... vertices) {
      List<ElevatorEdge> edges = new ArrayList<>();
      List<ElevatorOnboardVertex> onboardVertices = new ArrayList<>();

      for (Vertex v : vertices) {
        var level = String.format("L-%s", v.getDefaultName());
        var boardLabel = String.format("%s-onboard", level);
        var alightLabel = String.format("%s-offboard", level);

        var onboard = vertexFactory.elevatorOnboard(v, v.getLabelString(), boardLabel);
        var offboard = vertexFactory.elevatorOffboard(v, v.getLabelString(), alightLabel);

        FreeEdge.createFreeEdge(v, offboard);
        FreeEdge.createFreeEdge(offboard, v);

        edges.add(ElevatorBoardEdge.createElevatorBoardEdge(offboard, onboard));
        edges.add(
          ElevatorAlightEdge.createElevatorAlightEdge(
            onboard,
            offboard,
            new NonLocalizedString(level)
          )
        );

        onboardVertices.add(onboard);
      }

      for (int i = 1; i < onboardVertices.size(); i++) {
        var from = onboardVertices.get(i - 1);
        var to = onboardVertices.get(i);

        edges.add(
          ElevatorHopEdge.createElevatorHopEdge(from, to, permission, Accessibility.POSSIBLE)
        );
        edges.add(
          ElevatorHopEdge.createElevatorHopEdge(to, from, permission, Accessibility.POSSIBLE)
        );
      }

      return edges;
    }

    // -- Transit network (pathways, linking)
    public Entrance entranceEntity(String id, double latitude, double longitude) {
      return Entrance
        .of(TimetableRepositoryForTest.id(id))
        .withName(new NonLocalizedString(id))
        .withCode(id)
        .withCoordinate(latitude, longitude)
        .build();
    }

    RegularStop stopEntity(
      String id,
      double latitude,
      double longitude,
      @Nullable Station parentStation
    ) {
      var siteRepositoryBuilder = timetableRepository.getSiteRepository().withContext();
      var testModel = new TimetableRepositoryForTest(siteRepositoryBuilder);

      var stopBuilder = testModel.stop(id).withCoordinate(latitude, longitude);
      if (parentStation != null) {
        stopBuilder.withParentStation(parentStation);
      }

      var stop = stopBuilder.build();
      timetableRepository.mergeSiteRepositories(
        siteRepositoryBuilder.withRegularStop(stop).build()
      );
      return stop;
    }

    public Station stationEntity(String id, Consumer<StationBuilder> stationBuilder) {
      var siteRepositoryBuilder = timetableRepository.getSiteRepository().withContext();
      var testModel = new TimetableRepositoryForTest(siteRepositoryBuilder);

      var builder = testModel.station(id);
      stationBuilder.accept(builder);
      var station = builder.build();

      timetableRepository.mergeSiteRepositories(siteRepositoryBuilder.withStation(station).build());
      return station;
    }

    public TransitStopVertex stop(String id, WgsCoordinate coordinate, Station parentStation) {
      return stop(id, coordinate.latitude(), coordinate.longitude(), parentStation);
    }

    public TransitStopVertex stop(String id, WgsCoordinate coordinate) {
      return stop(id, coordinate, null);
    }

    public TransitStopVertex stop(String id, double latitude, double longitude) {
      return stop(id, latitude, longitude, null);
    }

    public TransitStopVertex stop(
      String id,
      double latitude,
      double longitude,
      @Nullable Station parentStation
    ) {
      return vertexFactory.transitStop(
        TransitStopVertex.of().withStop(stopEntity(id, latitude, longitude, parentStation))
      );
    }

    public TransitEntranceVertex entrance(String id, double latitude, double longitude) {
      return new TransitEntranceVertex(entranceEntity(id, latitude, longitude));
    }

    public StationCentroidVertex stationCentroid(Station station) {
      return vertexFactory.stationCentroid(station);
    }

    public StreetTransitEntranceLink link(StreetVertex from, TransitEntranceVertex to) {
      return StreetTransitEntranceLink.createStreetTransitEntranceLink(from, to);
    }

    public StreetTransitEntranceLink link(TransitEntranceVertex from, StreetVertex to) {
      return StreetTransitEntranceLink.createStreetTransitEntranceLink(from, to);
    }

    public List<StreetTransitEntranceLink> biLink(StreetVertex from, TransitEntranceVertex to) {
      return List.of(link(from, to), link(to, from));
    }

    public StreetTransitStopLink link(StreetVertex from, TransitStopVertex to) {
      return StreetTransitStopLink.createStreetTransitStopLink(from, to);
    }

    public StreetTransitStopLink link(TransitStopVertex from, StreetVertex to) {
      return StreetTransitStopLink.createStreetTransitStopLink(from, to);
    }

    public List<StreetTransitStopLink> biLink(StreetVertex from, TransitStopVertex to) {
      return List.of(link(from, to), link(to, from));
    }

    public StreetStationCentroidLink link(StreetVertex from, StationCentroidVertex to) {
      return StreetStationCentroidLink.createStreetStationLink(from, to);
    }

    public StreetStationCentroidLink link(StationCentroidVertex from, StreetVertex to) {
      return StreetStationCentroidLink.createStreetStationLink(from, to);
    }

    public List<StreetStationCentroidLink> biLink(StreetVertex from, StationCentroidVertex to) {
      return List.of(link(from, to), link(to, from));
    }

    public PathwayEdge pathway(Vertex from, Vertex to, int time, int length) {
      return PathwayEdge.createPathwayEdge(
        from,
        to,
        new NonLocalizedString(
          String.format("%s%s pathway", from.getDefaultName(), to.getDefaultName())
        ),
        time,
        length,
        0,
        0,
        false,
        PathwayMode.WALKWAY
      );
    }

    // -- Street linking
    public TemporaryStreetLocation streetLocation(
      String name,
      double latitude,
      double longitude,
      boolean endVertex
    ) {
      var nearestPoint = new Coordinate(longitude, latitude);
      return new TemporaryStreetLocation(name, nearestPoint, new NonLocalizedString(name));
    }

    public TemporaryFreeEdge link(TemporaryVertex from, StreetVertex to) {
      return TemporaryFreeEdge.createTemporaryFreeEdge(from, to);
    }

    public TemporaryFreeEdge link(StreetVertex from, TemporaryVertex to) {
      return TemporaryFreeEdge.createTemporaryFreeEdge(from, to);
    }

    // -- Vehicle rental
    public VehicleRentalPlace vehicleRentalStationEntity(
      String id,
      double latitude,
      double longitude,
      String network
    ) {
      var vehicleRentalStation = new VehicleRentalStation();
      vehicleRentalStation.id = new FeedScopedId(network, id);
      vehicleRentalStation.name = new NonLocalizedString(id);
      vehicleRentalStation.longitude = longitude;
      vehicleRentalStation.latitude = latitude;
      vehicleRentalStation.vehiclesAvailable = 2;
      vehicleRentalStation.spacesAvailable = 2;
      final RentalVehicleType vehicleType = RentalVehicleType.getDefaultType(network);
      vehicleRentalStation.vehicleTypesAvailable = Map.of(vehicleType, 2);
      vehicleRentalStation.vehicleSpacesAvailable = Map.of(vehicleType, 2);
      vehicleRentalStation.isArrivingInRentalVehicleAtDestinationAllowed = false;
      return vehicleRentalStation;
    }

    public VehicleRentalPlaceVertex vehicleRentalStation(
      String id,
      double latitude,
      double longitude,
      String network
    ) {
      var vertex = new VehicleRentalPlaceVertex(
        vehicleRentalStationEntity(id, latitude, longitude, network)
      );
      VehicleRentalEdge.createVehicleRentalEdge(
        vertex,
        RentalVehicleType.getDefaultType(network).formFactor
      );
      return vertex;
    }

    public VehicleRentalPlaceVertex vehicleRentalStation(
      String id,
      double latitude,
      double longitude
    ) {
      return vehicleRentalStation(id, latitude, longitude, TEST_VEHICLE_RENTAL_NETWORK);
    }

    public StreetVehicleRentalLink link(StreetVertex from, VehicleRentalPlaceVertex to) {
      return StreetVehicleRentalLink.createStreetVehicleRentalLink(from, to);
    }

    public StreetVehicleRentalLink link(VehicleRentalPlaceVertex from, StreetVertex to) {
      return StreetVehicleRentalLink.createStreetVehicleRentalLink(from, to);
    }

    public List<StreetVehicleRentalLink> biLink(StreetVertex from, VehicleRentalPlaceVertex to) {
      return List.of(link(from, to), link(to, from));
    }

    public VehicleParking vehicleParking(
      String id,
      double x,
      double y,
      boolean bicyclePlaces,
      boolean carPlaces,
      List<VehicleParkingEntranceCreator> entrances,
      String... tags
    ) {
      return vehicleParking(id, x, y, bicyclePlaces, carPlaces, false, entrances, tags);
    }

    public VehicleParking vehicleParking(
      String id,
      double x,
      double y,
      boolean bicyclePlaces,
      boolean carPlaces,
      boolean wheelchairAccessibleCarPlaces,
      List<VehicleParkingEntranceCreator> entrances,
      String... tags
    ) {
      var vehicleParking = VehicleParking
        .builder()
        .id(TimetableRepositoryForTest.id(id))
        .coordinate(new WgsCoordinate(y, x))
        .bicyclePlaces(bicyclePlaces)
        .carPlaces(carPlaces)
        .entrances(entrances)
        .wheelchairAccessibleCarPlaces(wheelchairAccessibleCarPlaces)
        .tags(List.of(tags))
        .build();

      var vertices = vehicleParkingHelper.createVehicleParkingVertices(vehicleParking);
      VehicleParkingHelper.linkVehicleParkingEntrances(vertices);
      vertices.forEach(v -> biLink(v.getParkingEntrance().getVertex(), v));
      return vehicleParking;
    }

    public VehicleParking.VehicleParkingEntranceCreator vehicleParkingEntrance(
      StreetVertex streetVertex,
      String id,
      boolean carAccessible,
      boolean walkAccessible
    ) {
      return builder ->
        builder
          .entranceId(TimetableRepositoryForTest.id(id))
          .name(new NonLocalizedString(id))
          .coordinate(new WgsCoordinate(streetVertex.getCoordinate()))
          .vertex(streetVertex)
          .carAccessible(carAccessible)
          .walkAccessible(walkAccessible);
    }

    public StreetVehicleParkingLink link(StreetVertex from, VehicleParkingEntranceVertex to) {
      return StreetVehicleParkingLink.createStreetVehicleParkingLink(from, to);
    }

    public StreetVehicleParkingLink link(VehicleParkingEntranceVertex from, StreetVertex to) {
      return StreetVehicleParkingLink.createStreetVehicleParkingLink(from, to);
    }

    public List<StreetVehicleParkingLink> biLink(
      StreetVertex from,
      VehicleParkingEntranceVertex to
    ) {
      return List.of(link(from, to), link(to, from));
    }

    public Route route(String id, TransitMode mode, Agency agency) {
      return TimetableRepositoryForTest.route(id).withAgency(agency).withMode(mode).build();
    }

    // Transit
    public void tripPattern(TripPattern tripPattern) {
      timetableRepository.addTripPattern(tripPattern.getId(), tripPattern);
    }

    public StopTime st(TransitStopVertex s1) {
      var st = new StopTime();
      st.setStop(s1.getStop());
      return st;
    }

    public StopTime st(TransitStopVertex s1, boolean board, boolean alight) {
      var st = new StopTime();
      st.setStop(s1.getStop());
      st.setPickupType(board ? PickDrop.SCHEDULED : PickDrop.NONE);
      st.setDropOffType(alight ? PickDrop.SCHEDULED : PickDrop.NONE);
      return st;
    }
  }
}
