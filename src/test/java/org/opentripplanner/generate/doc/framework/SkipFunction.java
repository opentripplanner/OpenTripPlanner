package org.opentripplanner.generate.doc.framework;

import java.util.Optional;
import org.opentripplanner.standalone.config.framework.json.NodeInfo;

/**
 * When generating the configuration we might want to stop processing the node three at some
 * point.
 * <ul>
 *   <li>
 *     In the {@link ParameterSummaryTable} we include "skipped" nodes and if provided we add a link,
 *     but children is not listed.
 *   </li>
 *   <li>
 *     In the {@link ParameterDetailsList} we skip "skipped" nodes.
 *   </li>
 * </ul>
 */

public interface SkipFunction {
  /**
   * Return true is node should be skipped (skip children for {@link ParameterSummaryTable}).
   */
  boolean skip(NodeInfo node);

  /**
   * Link to document where this node is described.
   */
  Optional<String> linkToDoc(NodeInfo node);
}
