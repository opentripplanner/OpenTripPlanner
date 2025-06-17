package org.opentripplanner.street.model._data;

import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.util.HashSet;
import java.util.Set;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.geometry.SplitLineString;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.DisposableEdgeCollection;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehiclerental.model.TestFreeFloatingRentalVehicleBuilder;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.AreaEdgeBuilder;
import org.opentripplanner.street.model.edge.AreaGroup;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;
import org.opentripplanner.street.model.edge.TemporaryFreeEdge;
import org.opentripplanner.street.model.edge.TemporaryPartialStreetEdge;
import org.opentripplanner.street.model.edge.TemporaryPartialStreetEdgeBuilder;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.LabelledIntersectionVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.TransitEntranceVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model.site.Entrance;

public class StreetModelForTest {

  public static StreetVertex V1 = intersectionVertex("V1", 0, 0);
  public static StreetVertex V2 = intersectionVertex("V2", 1, 1);
  public static StreetVertex V3 = intersectionVertex("V3", 2, 2);
  public static StreetVertex V4 = intersectionVertex("V4", 3, 3);

  public static IntersectionVertex intersectionVertex(Coordinate c) {
    return intersectionVertex(c.y, c.x);
  }

  public static IntersectionVertex intersectionVertex(double lat, double lon) {
    var label = "%s_%s".formatted(lat, lon);
    return new LabelledIntersectionVertex(label, lon, lat, false, false);
  }

  public static IntersectionVertex intersectionVertex(String label, double lat, double lon) {
    return new LabelledIntersectionVertex(label, lon, lat, false, false);
  }

  public static TransitEntranceVertex transitEntranceVertex(String id, double lat, double lon) {
    var entrance = Entrance.of(id(id))
      .withCoordinate(new WgsCoordinate(lat, lon))
      .withName(I18NString.of(id))
      .build();
    return new TransitEntranceVertex(entrance);
  }

  public static StreetEdge streetEdge(StreetVertex vA, StreetVertex vB) {
    var meters = SphericalDistanceLibrary.distance(vA.getCoordinate(), vB.getCoordinate());
    return streetEdge(vA, vB, meters, StreetTraversalPermission.ALL);
  }

  public static StreetEdgeBuilder<?> streetEdgeBuilder(
    StreetVertex vA,
    StreetVertex vB,
    double length,
    StreetTraversalPermission perm
  ) {
    var labelA = vA.getLabel();
    var labelB = vB.getLabel();
    String name = String.format("%s_%s", labelA, labelB);
    Coordinate[] coords = new Coordinate[2];
    coords[0] = vA.getCoordinate();
    coords[1] = vB.getCoordinate();
    LineString geom = GeometryUtils.getGeometryFactory().createLineString(coords);

    return new StreetEdgeBuilder<>()
      .withFromVertex(vA)
      .withToVertex(vB)
      .withGeometry(geom)
      .withName(name)
      .withMeterLength(length)
      .withPermission(perm)
      .withBack(false);
  }

  public static StreetEdge streetEdge(
    StreetVertex vA,
    StreetVertex vB,
    double length,
    StreetTraversalPermission perm
  ) {
    return streetEdgeBuilder(vA, vB, length, perm).buildAndConnect();
  }

  public static StreetEdge areaEdge(
    StreetVertex vA,
    StreetVertex vB,
    String name,
    StreetTraversalPermission perm
  ) {
    Coordinate[] coords = new Coordinate[2];
    coords[0] = vA.getCoordinate();
    coords[1] = vB.getCoordinate();
    LineString geom = GeometryUtils.getGeometryFactory().createLineString(coords);

    AreaGroup AREA = new AreaGroup(null);

    return new AreaEdgeBuilder()
      .withFromVertex(vA)
      .withToVertex(vB)
      .withGeometry(geom)
      .withPermission(perm)
      .withName(name)
      .withArea(AREA)
      .buildAndConnect();
  }

  public static StreetEdge streetEdge(
    StreetVertex from,
    StreetVertex to,
    StreetTraversalPermission permissions
  ) {
    return streetEdge(from, to, 1, permissions);
  }

  public static VehicleRentalPlaceVertex rentalVertex(RentalFormFactor formFactor) {
    var rentalVehicleBuilder = TestFreeFloatingRentalVehicleBuilder.of()
      .withLatitude(-122.575133)
      .withLongitude(45.456773);
    if (formFactor == RentalFormFactor.SCOOTER) {
      rentalVehicleBuilder.withVehicleScooter();
    } else if (formFactor == RentalFormFactor.BICYCLE) {
      rentalVehicleBuilder.withVehicleBicycle();
    } else if (formFactor == RentalFormFactor.CAR) {
      rentalVehicleBuilder.withVehicleCar();
    }
    return new VehicleRentalPlaceVertex(rentalVehicleBuilder.build());
  }

  public static VehicleParking.VehicleParkingBuilder vehicleParking() {
    return VehicleParking.builder().id(id("vehicle-parking-1")).coordinate(WgsCoordinate.GREENWICH);
  }

  static class GraphBuilder {

    Set<Vertex> knownVertices;
    Set<Vertex> inProgressVertices;
    Graph graph;

    public GraphBuilder() {
      knownVertices = new HashSet<>();
      inProgressVertices = new HashSet<>();
      graph = new Graph();
    }

    void process(Vertex vertex) {
      graph.addVertex(vertex);
      for (Edge edge : vertex.getOutgoing()) {
        add(edge.getToVertex());
      }
      for (Edge edge : vertex.getIncoming()) {
        add(edge.getFromVertex());
      }
    }

    public void add(Vertex vertex) {
      if (!knownVertices.contains(vertex)) {
        if (!inProgressVertices.contains(vertex)) {
          inProgressVertices.add(vertex);
          process(vertex);
        }
      }
    }

    public Graph build() {
      return graph;
    }
  }

  public static Graph makeGraph(Vertex vertex) {
    GraphBuilder builder = new GraphBuilder();
    builder.add(vertex);
    return builder.build();
  }

  /**
   * Creates a TemporaryStreetLocation on the given street (set of PlainStreetEdges). How far along
   * is controlled by the location parameter, which represents a distance along the edge between 0
   * (the from vertex) and 1 (the to vertex).
   *
   * @param edges A collection of nearby edges, which represent one street.
   * @return the new TemporaryStreetLocation
   */
  public static TemporaryStreetLocation createTemporaryStreetLocationForTest(
    String label,
    I18NString name,
    Iterable<StreetEdge> edges,
    Coordinate nearestPoint,
    boolean endVertex,
    DisposableEdgeCollection tempEdges
  ) {
    boolean wheelchairAccessible = false;

    TemporaryStreetLocation location = new TemporaryStreetLocation(nearestPoint, name);

    for (StreetEdge street : edges) {
      Vertex fromv = street.getFromVertex();
      Vertex tov = street.getToVertex();
      wheelchairAccessible |= street.isWheelchairAccessible();

      /* forward edges and vertices */
      Vertex edgeLocation;
      if (SphericalDistanceLibrary.distance(nearestPoint, fromv.getCoordinate()) < 1) {
        // no need to link to area edges caught on-end
        edgeLocation = fromv;

        if (endVertex) {
          tempEdges.addEdge(TemporaryFreeEdge.createTemporaryFreeEdge(edgeLocation, location));
        } else {
          tempEdges.addEdge(TemporaryFreeEdge.createTemporaryFreeEdge(location, edgeLocation));
        }
      } else if (SphericalDistanceLibrary.distance(nearestPoint, tov.getCoordinate()) < 1) {
        // no need to link to area edges caught on-end
        edgeLocation = tov;

        if (endVertex) {
          tempEdges.addEdge(TemporaryFreeEdge.createTemporaryFreeEdge(edgeLocation, location));
        } else {
          tempEdges.addEdge(TemporaryFreeEdge.createTemporaryFreeEdge(location, edgeLocation));
        }
      } else {
        // creates links from street head -> location -> street tail.
        createHalfLocationForTest(location, name, nearestPoint, street, endVertex, tempEdges);
      }
    }
    location.setWheelchairAccessible(wheelchairAccessible);
    return location;
  }

  private static void createHalfLocationForTest(
    TemporaryStreetLocation base,
    I18NString name,
    Coordinate nearestPoint,
    StreetEdge street,
    boolean endVertex,
    DisposableEdgeCollection tempEdges
  ) {
    StreetVertex tov = (StreetVertex) street.getToVertex();
    StreetVertex fromv = (StreetVertex) street.getFromVertex();
    LineString geometry = street.getGeometry();

    SplitLineString geometries = getGeometry(street, nearestPoint);

    double totalGeomLength = geometry.getLength();
    double lengthRatioIn = geometries.beginning().getLength() / totalGeomLength;

    double lengthIn = street.getDistanceMeters() * lengthRatioIn;
    double lengthOut = street.getDistanceMeters() * (1 - lengthRatioIn);

    if (endVertex) {
      TemporaryPartialStreetEdge tpse = new TemporaryPartialStreetEdgeBuilder()
        .withParentEdge(street)
        .withFromVertex(fromv)
        .withToVertex(base)
        .withGeometry(geometries.beginning())
        .withName(name)
        .withMeterLength(lengthIn)
        .withMotorVehicleNoThruTraffic(street.isMotorVehicleNoThruTraffic())
        .withBicycleNoThruTraffic(street.isBicycleNoThruTraffic())
        .withWalkNoThruTraffic(street.isWalkNoThruTraffic())
        .withLink(street.isLink())
        .buildAndConnect();
      tempEdges.addEdge(tpse);
    } else {
      TemporaryPartialStreetEdge tpse = new TemporaryPartialStreetEdgeBuilder()
        .withParentEdge(street)
        .withFromVertex(base)
        .withToVertex(tov)
        .withGeometry(geometries.ending())
        .withName(name)
        .withMeterLength(lengthOut)
        .withLink(street.isLink())
        .withMotorVehicleNoThruTraffic(street.isMotorVehicleNoThruTraffic())
        .withBicycleNoThruTraffic(street.isBicycleNoThruTraffic())
        .withWalkNoThruTraffic(street.isWalkNoThruTraffic())
        .buildAndConnect();
      tempEdges.addEdge(tpse);
    }
  }

  private static SplitLineString getGeometry(StreetEdge e, Coordinate nearestPoint) {
    LineString geometry = e.getGeometry();
    return GeometryUtils.splitGeometryAtPoint(geometry, nearestPoint);
  }
}
