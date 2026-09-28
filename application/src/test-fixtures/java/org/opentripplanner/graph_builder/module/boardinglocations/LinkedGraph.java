package org.opentripplanner.graph_builder.module.boardinglocations;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.service.osminfo.internal.DefaultOsmInfoGraphBuildRepository;
import org.opentripplanner.service.osminfo.internal.DefaultOsmInfoGraphBuildService;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.OsmBoardingLocationVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model.site.RegularStop;

/** The graph a {@link BoardingLocationsEnvironment} produced, with the lookups tests need. */
public record LinkedGraph(
  Graph graph,
  DefaultOsmInfoGraphBuildRepository osmInfoRepository,
  DefaultDataImportIssueStore issueStore
) {
  private TransitStopVertex stopVertex(RegularStop stop) {
    return graph
      .getVerticesOfType(TransitStopVertex.class)
      .stream()
      .filter(v -> v.getId().equals(stop.getId()))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("no transit stop vertex for " + stop.getId()));
  }

  public Set<Vertex> linkedVertices(RegularStop stop) {
    return stopVertex(stop)
      .getOutgoing()
      .stream()
      .map(Edge::getToVertex)
      .collect(Collectors.toSet());
  }

  /** The single boarding location the stop is linked to. */
  public OsmBoardingLocationVertex boardingLocation(RegularStop stop) {
    var linked = linkedVertices(stop);
    if (linked.size() != 1) {
      throw new IllegalStateException(
        stop.getId() + " should link to exactly one boarding location, but linked to " + linked
      );
    }
    var vertex = linked.iterator().next();
    if (vertex instanceof OsmBoardingLocationVertex boardingLocation) {
      return boardingLocation;
    }
    throw new IllegalStateException(
      stop.getId() + " is linked to a " + vertex.getClass().getSimpleName()
    );
  }

  /** The street edges a boarding location walks out over, excluding the link to its stop. */
  public List<StreetEdge> connectors(RegularStop stop) {
    return boardingLocation(stop)
      .getOutgoing()
      .stream()
      .filter(StreetEdge.class::isInstance)
      .map(StreetEdge.class::cast)
      .toList();
  }

  /** The vertices a boarding location walks to, over its connector edges. */
  public Set<Vertex> attachmentPoints(RegularStop stop) {
    return connectors(stop).stream().map(Edge::getToVertex).collect(Collectors.toSet());
  }

  /** How many visibility edges a boarding location has into its platform area. */
  public long areaEdgeCount(Vertex vertex) {
    return vertex.getOutgoing().stream().filter(AreaEdge.class::isInstance).count();
  }

  /** How far the boarding location ended up from the stop's own coordinate. */
  public double distanceFromStop(RegularStop stop) {
    return SphericalDistanceLibrary.distance(
      boardingLocation(stop).getCoordinate(),
      stop.getCoordinate().asJtsCoordinate()
    );
  }

  /** How many street edges in the whole graph are registered as belonging to a platform. */
  public long platformEdgeCount() {
    var service = osmInfoService();
    return StreamSupport.stream(graph.findEdges(StreetEdge.class).spliterator(), false)
      .filter(edge -> service.findPlatform(edge).isPresent())
      .count();
  }

  public List<String> issueTypes() {
    return issueStore.listIssues().stream().map(DataImportIssue::getType).toList();
  }

  public DefaultOsmInfoGraphBuildService osmInfoService() {
    return new DefaultOsmInfoGraphBuildService(osmInfoRepository);
  }
}
