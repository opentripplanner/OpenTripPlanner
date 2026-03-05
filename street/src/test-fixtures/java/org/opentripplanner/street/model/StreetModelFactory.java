package org.opentripplanner.street.model;

import static org.opentripplanner.core.model.id.FeedScopedIdFactory.id;

import java.time.Instant;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.core.model.accessibility.Accessibility;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehiclerental.model.TestFreeFloatingRentalVehicleBuilder;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.geometry.SplitLineString;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.edge.AreaEdgeBuilder;
import org.opentripplanner.street.model.edge.AreaGroup;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;
import org.opentripplanner.street.model.edge.TemporaryPartialStreetEdgeBuilder;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.LabelledIntersectionVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.TransitEntranceVertex;

public class StreetModelFactory {

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
    return new TransitEntranceVertex(
      id(id),
      new WgsCoordinate(lat, lon),
      I18NString.of(id),
      Accessibility.NO_INFORMATION
    );
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

  public static VehicleRentalPlaceVertex rentalVertex(
    RentalFormFactor formFactor,
    Instant availableUntil
  ) {
    TestFreeFloatingRentalVehicleBuilder vehicleBuilder = getTestRentalVehicleBuilder(
      formFactor
    ).withAvailableUntil(availableUntil);
    return new VehicleRentalPlaceVertex(vehicleBuilder.build());
  }

  public static VehicleParking.VehicleParkingBuilder vehicleParking() {
    return VehicleParking.builder().id(id("vehicle-parking-1")).coordinate(WgsCoordinate.GREENWICH);
  }

  public static VehicleRentalPlaceVertex rentalVertex(RentalFormFactor formFactor) {
    var rentalVehicleBuilder = getTestRentalVehicleBuilder(formFactor);
    return new VehicleRentalPlaceVertex(rentalVehicleBuilder.build());
  }

  private static TestFreeFloatingRentalVehicleBuilder getTestRentalVehicleBuilder(
    RentalFormFactor formFactor
  ) {
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
    return rentalVehicleBuilder;
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
    I18NString name,
    Iterable<StreetEdge> edges,
    Coordinate nearestPoint,
    boolean endVertex
  ) {
    TemporaryStreetLocation location = new TemporaryStreetLocation(nearestPoint, name);

    for (StreetEdge street : edges) {
      /* forward edges and vertices */
      createHalfLocationForTest(location, name, nearestPoint, street, endVertex);
    }
    return location;
  }

  private static void createHalfLocationForTest(
    TemporaryStreetLocation base,
    I18NString name,
    Coordinate nearestPoint,
    StreetEdge street,
    boolean endVertex
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
      new TemporaryPartialStreetEdgeBuilder()
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
    } else {
      new TemporaryPartialStreetEdgeBuilder()
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
    }
  }

  private static SplitLineString getGeometry(StreetEdge e, Coordinate nearestPoint) {
    LineString geometry = e.getGeometry();
    return GeometryUtils.splitGeometryAtPoint(geometry, nearestPoint);
  }
}
