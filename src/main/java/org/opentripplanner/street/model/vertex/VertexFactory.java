package org.opentripplanner.street.model.vertex;

import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.openstreetmap.model.OSMNode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingEntrance;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.transit.model.site.BoardingArea;
import org.opentripplanner.transit.model.site.Entrance;
import org.opentripplanner.transit.model.site.PathwayNode;

/**
 * This class is the central point where all vertices that are supposed to be permanently part
 * of the graph are built. It is the responsibility of this class to make sure that the vertices
 * are added to the graph.
 * <p>
 * Vertices that are not supposed to be part of the graph, like temporary splitter vertices, don't
 * need to be added to the graph and hence are not listed here.
 */
public class VertexFactory {

  private final Graph graph;

  public VertexFactory(Graph graph) {
    this.graph = graph;
  }

  @Nonnull
  public TransitBoardingAreaVertex transitBoardingArea(BoardingArea boardingArea) {
    return addToGraph(new TransitBoardingAreaVertex(boardingArea));
  }

  @Nonnull
  public ElevatorOnboardVertex elevatorOnboard(
    Vertex sourceVertex,
    String label,
    String levelName
  ) {
    return addToGraph(new ElevatorOnboardVertex(sourceVertex, label, levelName));
  }

  @Nonnull
  public ElevatorOffboardVertex elevatorOffboard(
    Vertex sourceVertex,
    String label,
    String levelName
  ) {
    return addToGraph(new ElevatorOffboardVertex(sourceVertex, label, levelName));
  }

  @Nonnull
  public IntersectionVertex intersection(Coordinate edgeCoordinate) {
    return addToGraph(
      new LabelledIntersectionVertex(
        "area splitter at " + edgeCoordinate,
        edgeCoordinate.x,
        edgeCoordinate.y,
        false,
        false
      )
    );
  }

  @Nonnull
  public IntersectionVertex intersection(String label, double longitude, double latitude) {
    return addToGraph(new LabelledIntersectionVertex(label, longitude, latitude, false, false));
  }

  @Nonnull
  public OsmBoardingLocationVertex osmBoardingLocation(
    Coordinate coordinate,
    String label,
    Set<String> refs,
    @Nullable I18NString name
  ) {
    return addToGraph(new OsmBoardingLocationVertex(label, coordinate.x, coordinate.y, name, refs));
  }

  @Nonnull
  public SplitterVertex splitter(
    StreetEdge originalEdge,
    double x,
    double y,
    String uniqueSplitLabel
  ) {
    return addToGraph(new SplitterVertex(uniqueSplitLabel, x, y, originalEdge.getName()));
  }

  @Nonnull
  public BarrierVertex barrier(long nid, Coordinate coordinate) {
    return addToGraph(new BarrierVertex(coordinate.x, coordinate.y, nid));
  }

  @Nonnull
  public ExitVertex exit(long nid, Coordinate coordinate, String exitName) {
    return addToGraph(new ExitVertex(coordinate.x, coordinate.y, nid, exitName));
  }

  @Nonnull
  public OsmVertex osm(
    Coordinate coordinate,
    OSMNode node,
    boolean highwayTrafficLight,
    boolean crossingTrafficLight
  ) {
    return addToGraph(
      new OsmVertex(
        coordinate.x,
        coordinate.y,
        node.getId(),
        highwayTrafficLight,
        crossingTrafficLight
      )
    );
  }

  @Nonnull
  public TransitStopVertex transitStop(TransitStopVertexBuilder transitStopVertexBuilder) {
    return addToGraph(transitStopVertexBuilder.build());
  }

  @Nonnull
  public VehicleParkingEntranceVertex vehicleParkingEntrance(VehicleParking vehicleParking) {
    return vehicleParkingEntrance(vehicleParking.getEntrances().get(0));
  }

  public VehicleParkingEntranceVertex vehicleParkingEntrance(VehicleParkingEntrance entrance) {
    return addToGraph(new VehicleParkingEntranceVertex(entrance));
  }

  @Nonnull
  public VehicleRentalPlaceVertex vehicleRentalPlace(VehicleRentalPlace station) {
    return addToGraph(new VehicleRentalPlaceVertex(station));
  }

  @Nonnull
  public TransitPathwayNodeVertex transitPathwayNode(PathwayNode node) {
    return addToGraph(new TransitPathwayNodeVertex(node));
  }

  @Nonnull
  public TransitEntranceVertex transitEntrance(Entrance entrance) {
    return addToGraph(new TransitEntranceVertex(entrance));
  }

  public OsmVertex levelledOsm(OSMNode node, String level) {
    return addToGraph(new OsmVertexOnLevel(node, level));
  }

  private <T extends Vertex> T addToGraph(T vertex) {
    graph.addVertex(vertex);
    return vertex;
  }
}
