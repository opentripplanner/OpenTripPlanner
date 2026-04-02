package org.opentripplanner.street.integration;

import static org.opentripplanner.core.model.id.FeedScopedIdFactory.id;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.core.model.accessibility.Accessibility;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehicleparking.model.VehicleParking.VehicleParkingEntranceCreator;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.service.vehiclerental.street.StreetVehicleRentalLink;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalEdge;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.linking.VehicleParkingHelper;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.ElevatorAlightEdge;
import org.opentripplanner.street.model.edge.ElevatorBoardEdge;
import org.opentripplanner.street.model.edge.ElevatorEdge;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;
import org.opentripplanner.street.model.edge.PathwayEdge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;
import org.opentripplanner.street.model.edge.StreetTransitEntranceLink;
import org.opentripplanner.street.model.edge.StreetTransitStopLink;
import org.opentripplanner.street.model.edge.StreetVehicleParkingLink;
import org.opentripplanner.street.model.edge.TemporaryFreeEdge;
import org.opentripplanner.street.model.vertex.ElevatorHopVertex;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.LabelledIntersectionVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.TemporaryVertex;
import org.opentripplanner.street.model.vertex.TransitEntranceVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.street.model.vertex.Vertex;

abstract class GraphRoutingTest {

  public static final String TEST_VEHICLE_RENTAL_NETWORK = "test network";

  protected Graph modelOf(Builder builder) {
    builder.build();
    return builder.graph;
  }

  public abstract static class Builder {

    private final Graph graph;
    private final VehicleParkingHelper vehicleParkingHelper;

    protected Builder() {
      graph = new Graph();
      vehicleParkingHelper = new VehicleParkingHelper(graph);
    }

    public abstract void build();

    // -- Street network
    public IntersectionVertex intersection(String label, double latitude, double longitude) {
      return new LabelledIntersectionVertex(label, longitude, latitude, false, false);
    }

    public StreetEdge street(
      StreetVertex from,
      StreetVertex to,
      int length,
      StreetTraversalPermission permissions
    ) {
      return streetBuilder(from, to, length, permissions).buildAndConnect();
    }

    public StreetEdge street(
      StreetVertex from,
      StreetVertex to,
      int length,
      StreetTraversalPermission permissions,
      float carSpeed
    ) {
      return streetBuilder(from, to, length, permissions).withCarSpeed(carSpeed).buildAndConnect();
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
      List<ElevatorHopVertex> onboardVertices = new ArrayList<>();

      for (int i = 0; i < vertices.length; i++) {
        Vertex v = vertices[i];

        var onboard = new ElevatorHopVertex(v, v.getLabelString() + "_" + i);

        edges.add(ElevatorBoardEdge.createElevatorBoardEdge(v, onboard));
        edges.add(ElevatorAlightEdge.createElevatorAlightEdge(onboard, v));

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
    public TransitStopVertex stop(String id, double lat, double lon) {
      var x = TransitStopVertex.of()
        .withId(id(id))
        .withPoint(GeometryUtils.getGeometryFactory().createPoint(new Coordinate(lon, lat)));
      return x.build();
    }

    public TransitEntranceVertex entrance(String id, double latitude, double longitude) {
      return new TransitEntranceVertex(
        id(id),
        new WgsCoordinate(latitude, longitude),
        I18NString.of(id),
        Accessibility.NO_INFORMATION
      );
    }

    public List<StreetTransitEntranceLink> biLink(StreetVertex from, TransitEntranceVertex to) {
      return List.of(
        StreetTransitEntranceLink.createStreetTransitEntranceLink(from, to),
        StreetTransitEntranceLink.createStreetTransitEntranceLink(to, from)
      );
    }

    public List<StreetTransitStopLink> biLink(StreetVertex from, TransitStopVertex to) {
      return List.of(
        StreetTransitStopLink.createStreetTransitStopLink(from, to),
        StreetTransitStopLink.createStreetTransitStopLink(to, from)
      );
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
        false
      );
    }

    // -- Street linking

    public TemporaryStreetLocation streetLocation(String name, double latitude, double longitude) {
      var nearestPoint = new Coordinate(longitude, latitude);
      return new TemporaryStreetLocation(nearestPoint, new NonLocalizedString(name));
    }

    public TemporaryFreeEdge link(TemporaryVertex from, StreetVertex to) {
      return TemporaryFreeEdge.createTemporaryFreeEdge(from, to);
    }

    public TemporaryFreeEdge link(StreetVertex from, TemporaryVertex to) {
      return TemporaryFreeEdge.createTemporaryFreeEdge(from, to);
    }

    // -- Vehicle rental

    public VehicleRentalPlaceVertex vehicleRentalStation(
      String id,
      double latitude,
      double longitude
    ) {
      var vertex = new VehicleRentalPlaceVertex(
        vehicleRentalStationEntity(id, latitude, longitude, TEST_VEHICLE_RENTAL_NETWORK)
      );
      VehicleRentalEdge.createVehicleRentalEdge(
        vertex,
        RentalVehicleType.getDefaultType(TEST_VEHICLE_RENTAL_NETWORK).formFactor()
      );
      return vertex;
    }

    public List<StreetVehicleRentalLink> biLink(StreetVertex from, VehicleRentalPlaceVertex to) {
      return List.of(
        StreetVehicleRentalLink.createStreetVehicleRentalLink(from, to),
        StreetVehicleRentalLink.createStreetVehicleRentalLink(to, from)
      );
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
      var vehicleParking = VehicleParking.builder()
        .id(id(id))
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

    public VehicleParkingEntranceCreator vehicleParkingEntrance(
      StreetVertex streetVertex,
      String id,
      boolean carAccessible,
      boolean walkAccessible
    ) {
      return builder ->
        builder
          .entranceId(id(id))
          .name(new NonLocalizedString(id))
          .coordinate(new WgsCoordinate(streetVertex.getCoordinate()))
          .vertex(streetVertex)
          .carAccessible(carAccessible)
          .walkAccessible(walkAccessible);
    }

    private VehicleRentalPlace vehicleRentalStationEntity(
      String id,
      double latitude,
      double longitude,
      String network
    ) {
      final RentalVehicleType vehicleType = RentalVehicleType.getDefaultType(network);
      return VehicleRentalStation.of()
        .withId(new FeedScopedId(network, id))
        .withName(new NonLocalizedString(id))
        .withLongitude(longitude)
        .withLatitude(latitude)
        .withVehiclesAvailable(2)
        .withSpacesAvailable(2)
        .withVehicleTypesAvailable(Map.of(vehicleType, 2))
        .withVehicleSpacesAvailable(Map.of(vehicleType, 2))
        .withIsArrivingInRentalVehicleAtDestinationAllowed(false)
        .build();
    }

    private StreetEdgeBuilder<?> streetBuilder(
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

    private List<StreetVehicleParkingLink> biLink(
      StreetVertex from,
      VehicleParkingEntranceVertex to
    ) {
      return List.of(
        StreetVehicleParkingLink.createStreetVehicleParkingLink(from, to),
        StreetVehicleParkingLink.createStreetVehicleParkingLink(to, from)
      );
    }
  }
}
