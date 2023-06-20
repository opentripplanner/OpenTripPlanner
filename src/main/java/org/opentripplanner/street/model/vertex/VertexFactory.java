package org.opentripplanner.street.model.vertex;

import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
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
    String sourceVertexLabel,
    String levelName
  ) {
    return addToGraph(
      new ElevatorOnboardVertex(
        sourceVertexLabel + "_onboard",
        sourceVertex.getX(),
        sourceVertex.getY(),
        new NonLocalizedString(levelName)
      )
    );
  }

  @Nonnull
  public ElevatorOffboardVertex elevatorOffboard(
    Vertex sourceVertex,
    String sourceVertexLabel,
    String levelName
  ) {
    return addToGraph(
      new ElevatorOffboardVertex(
        sourceVertexLabel + "_offboard",
        sourceVertex.getX(),
        sourceVertex.getY(),
        new NonLocalizedString(levelName)
      )
    );
  }

  @Nonnull
  public IntersectionVertex intersection(Coordinate edgeCoordinate) {
    return addToGraph(
      new IntersectionVertex(
        "area splitter at " + edgeCoordinate,
        edgeCoordinate.x,
        edgeCoordinate.y
      )
    );
  }

  @Nonnull
  public IntersectionVertex intersection(String label, double longitude, double latitude) {
    return addToGraph(new IntersectionVertex(label, longitude, latitude));
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
  public BarrierVertex barrier(long nid, Coordinate coordinate, String label) {
    return addToGraph(new BarrierVertex(label, coordinate.x, coordinate.y, nid));
  }

  @Nonnull
  public ExitVertex exit(long nid, Coordinate coordinate, String label) {
    return addToGraph(new ExitVertex(label, coordinate.x, coordinate.y, nid));
  }

  @Nonnull
  public OsmVertex osm(
    String label,
    Coordinate coordinate,
    OSMNode node,
    boolean highwayTrafficLight,
    boolean crossingTrafficLight
  ) {
    return addToGraph(
      new OsmVertex(
        label,
        coordinate.x,
        coordinate.y,
        node.getId(),
        new NonLocalizedString(label),
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

  private <T extends Vertex> T addToGraph(T vertex) {
    graph.addVertex(vertex);
    return vertex;
  }
}
