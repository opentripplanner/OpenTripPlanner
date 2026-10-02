package org.opentripplanner.graph_builder.module.boardinglocations;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.service.osminfo.internal.DefaultOsmInfoGraphBuildRepository;
import org.opentripplanner.service.osminfo.internal.DefaultOsmInfoGraphBuildService;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.graph.summary.GraphSummarizer;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.OsmBoardingLocationVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.utils.collection.StreamUtils;

/** The graph a {@link BoardingLocationsEnvironment} produced, with the lookups tests need. */
public record LinkedGraph(
  Graph rawGraph,
  DefaultOsmInfoGraphBuildRepository osmInfoRepository,
  DefaultDataImportIssueStore issueStore
) {
  private TransitStopVertex stopVertex(RegularStop stop) {
    return rawGraph
      .findStopVertex(stop.getId())
      .orElseThrow(() -> new IllegalStateException("no transit stop vertex for " + stop.getId()));
  }

  /** Every vertex the stop is linked to. */
  public List<Vertex> linkedVertices(RegularStop stop) {
    return GraphSummarizer.successors(stopVertex(stop));
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

  /** The street edges leading out of a boarding location, excluding the link to its stop. */
  public List<StreetEdge> connectors(RegularStop stop) {
    return boardingLocation(stop).getOutgoingStreetEdges();
  }

  /** The vertices a boarding location is connected to, over its connector edges. */
  public List<Vertex> attachmentPoints(RegularStop stop) {
    return connectors(stop).stream().map(Edge::getToVertex).toList();
  }

  /** How many visibility edges a boarding location has into its platform area. */
  public long areaEdgeCount(Vertex vertex) {
    return GraphSummarizer.outgoingAreaEdges(vertex).size();
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
    return StreamUtils.ofIterable(rawGraph.findEdges(StreetEdge.class))
      .filter(edge -> service.findPlatform(edge).isPresent())
      .count();
  }

  /** Every edge in the graph as a readable string, to assert the shape of the linking against. */
  public Collection<String> summarizeEdges() {
    return new GraphSummarizer(rawGraph).summarizeEdges();
  }

  /** A link that draws the graph on a map, for the message of a failing {@link #summarizeEdges()}. */
  public String geoJsonUrl() {
    return new GraphSummarizer(rawGraph).geoJsonUrl();
  }

  public List<String> issueTypes() {
    return issueStore.listIssues().stream().map(DataImportIssue::getType).toList();
  }

  public DefaultOsmInfoGraphBuildService osmInfoService() {
    return new DefaultOsmInfoGraphBuildService(osmInfoRepository);
  }
}
