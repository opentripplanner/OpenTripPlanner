package org.opentripplanner.street.model.vertex;

import java.util.Set;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingEntrance;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.framework.FeedScopedId;
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

  public TransitBoardingAreaVertex transitBoardingArea(BoardingArea ba) {
    return addToGraph(
      new TransitBoardingAreaVertex(
        ba.getId(),
        ba.getCoordinate(),
        ba.getName(),
        ba.getWheelchairAccessibility()
      )
    );
  }

  public ElevatorVertex elevator(Vertex sourceVertex, String label, double level) {
    return addToGraph(new ElevatorVertex(sourceVertex, label, level));
  }

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

  public IntersectionVertex intersection(String label, double longitude, double latitude) {
    return addToGraph(new LabelledIntersectionVertex(label, longitude, latitude, false, false));
  }

  public OsmBoardingLocationVertex osmBoardingLocation(
    Coordinate coordinate,
    String label,
    Set<String> refs,
    @Nullable I18NString name
  ) {
    return addToGraph(new OsmBoardingLocationVertex(label, coordinate.x, coordinate.y, name, refs));
  }

  public SplitterVertex splitter(
    StreetEdge originalEdge,
    double x,
    double y,
    String uniqueSplitLabel
  ) {
    return addToGraph(new SplitterVertex(uniqueSplitLabel, x, y, originalEdge.getName()));
  }

  public BarrierVertex barrier(
    long nid,
    Coordinate coordinate,
    Accessibility wheelchairAccessibility
  ) {
    return addToGraph(new BarrierVertex(coordinate.x, coordinate.y, nid, wheelchairAccessibility));
  }

  public ExitVertex exit(long nid, Coordinate coordinate, String exitName) {
    return addToGraph(new ExitVertex(coordinate.x, coordinate.y, nid, exitName));
  }

  public StationEntranceVertex stationEntrance(
    long nid,
    Coordinate coordinate,
    String code,
    Accessibility wheelchairAccessibility
  ) {
    return addToGraph(
      new StationEntranceVertex(coordinate.x, coordinate.y, nid, code, wheelchairAccessibility)
    );
  }

  public OsmVertex osm(
    Coordinate coordinate,
    long nid,
    boolean highwayTrafficLight,
    boolean crossingTrafficLight
  ) {
    return addToGraph(
      new OsmVertex(coordinate.x, coordinate.y, nid, highwayTrafficLight, crossingTrafficLight)
    );
  }

  public OsmVertex osmOnLinearBarrier(Coordinate coordinate, long nid, long routableWayId) {
    return addToGraph(new BarrierPassThroughVertex(coordinate.x, coordinate.y, nid, routableWayId));
  }

  public TransitStopVertex transitStop(TransitStopVertexBuilder builder) {
    var v = builder.build();
    return addToGraph(v);
  }

  public StationCentroidVertex stationCentroid(FeedScopedId id, WgsCoordinate coordinate) {
    return addToGraph(new StationCentroidVertex(id, coordinate));
  }

  public VehicleParkingEntranceVertex vehicleParkingEntrance(VehicleParking vehicleParking) {
    return vehicleParkingEntrance(vehicleParking.getEntrances().get(0));
  }

  public VehicleParkingEntranceVertex vehicleParkingEntrance(VehicleParkingEntrance entrance) {
    return addToGraph(new VehicleParkingEntranceVertex(entrance));
  }

  public VehicleRentalPlaceVertex vehicleRentalPlace(VehicleRentalPlace station) {
    return addToGraph(new VehicleRentalPlaceVertex(station));
  }

  public TransitPathwayNodeVertex transitPathwayNode(PathwayNode node) {
    return addToGraph(
      new TransitPathwayNodeVertex(node.getId(), node.getCoordinate(), node.getName())
    );
  }

  public TransitEntranceVertex transitEntrance(Entrance entrance) {
    return addToGraph(
      new TransitEntranceVertex(
        entrance.getId(),
        entrance.getCoordinate(),
        entrance.getName(),
        entrance.getWheelchairAccessibility()
      )
    );
  }

  public OsmVertex levelledOsm(OsmNode node, double level) {
    return addToGraph(
      new OsmVertexOnLevel(node.getId(), new WgsCoordinate(node.getCoordinate()), level)
    );
  }

  private <T extends Vertex> T addToGraph(T vertex) {
    graph.addVertex(vertex);
    return vertex;
  }
}
