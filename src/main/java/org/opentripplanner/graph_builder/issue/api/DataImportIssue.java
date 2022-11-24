package org.opentripplanner.graph_builder.issue.api;

import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * Represents noteworthy data import issues that occur during the graph building process. These
 * issues should be passed on the the {@link DataImportIssueStore}
 * fwitch will be responsible for logging, summarizing and reporting the issue.
 * <p>
 * Do NOT log the issue in the class where the issue is detected/created.
 *
 * @author andrewbyrd
 */
public interface DataImportIssue {
  /**
   * The issue report is grouped by type name.
   */
  default String getType() {
    return getClass().getSimpleName();
  }

  /**
   * Provide a detailed message, including enough data to be able to fix the problem (in the source
   * system).
   */
  String getMessage();

  /**
   * This method is used by the HTML report builder. It is useful to put links to OSM here.
   */
  default String getHTMLMessage() {
    return this.getMessage();
  }

  /**
   * @deprecated This is used in the {@link org.opentripplanner.visualizer.ShowGraph} only, which
   * status is unclear. Is anyone still using it?
   */
  @Deprecated
  default Edge getReferencedEdge() {
    return null;
  }

  /**
   * @deprecated This is used in the {@link org.opentripplanner.visualizer.ShowGraph} only, which
   * status is unclear. Is anyone still using it?
   */
  @Deprecated
  default Vertex getReferencedVertex() {
    return null;
  }
}
