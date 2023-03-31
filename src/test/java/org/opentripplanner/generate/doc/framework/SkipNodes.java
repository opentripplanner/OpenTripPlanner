package org.opentripplanner.generate.doc.framework;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.opentripplanner.standalone.config.framework.json.NodeInfo;

/**
 * When generating the configuration we might want to stop processing the node tree at some
 * point. We do not have reusable types in the config, so instead we might document a node
 * in one place and then link to it if we are reusing the same structure in another. A little
 * duplication is ok(like updaters), but listing all parameters of the routingRequest in several
 * places is not.
 * <ul>
 *   <li>
 *     In the {@link ParameterSummaryTable} we include "skipped" nodes and if provided we add a
 *     link, but children is not listed.
 *   </li>
 *   <li>
 *     In the {@link ParameterDetailsList} we skip "skipped" nodes, but include "skipNestedElements".
 *     For "skipNestedElements", children is skipped.
 *   </li>
 * </ul>
 */
public class SkipNodes {

  private final Map<String, String> skipMap;
  private final Map<String, String> skipNestedElements;

  public SkipNodes(Map<String, String> skipMap, Map<String, String> skipNestedElements) {
    this.skipMap = Map.copyOf(skipMap);
    this.skipNestedElements = Map.copyOf(skipNestedElements);
  }

  public static Builder of() {
    return new Builder();
  }

  /**
   * Return {@code link} to use in overview for element skipped in
   * the detailed section.
   */
  public Optional<String> linkOverview(NodeInfo node) {
    return Optional.ofNullable(skipMap.get(node.name()));
  }

  /**
   * Return {@code true} if this element should not be included in the details section.
   */
  public boolean skipDetails(NodeInfo node) {
    return skipMap.containsKey(node.name());
  }

  /**
   * Return {@code link} to use in detailed section for element with skipped nested elements.
   */
  public Optional<String> linkDetails(NodeInfo node) {
    return Optional.ofNullable(skipNestedElements.get(node.name()));
  }

  /**
   * Return {@code true} if the nested children of this element should be skipped in the details section.
   */
  public boolean skipDetailsForNestedElements(NodeInfo node) {
    return skipNestedElements.containsKey(node.name());
  }

  public static class Builder {

    private final Map<String, String> skipMap = new HashMap<>();
    private final Map<String, String> skipNestedElements = new HashMap<>();

    public Builder skip(String parameterName, String link) {
      Objects.requireNonNull(parameterName);
      Objects.requireNonNull(link);
      skipMap.put(parameterName, link);
      return this;
    }

    public Builder skipNestedElements(String parameterName, String link) {
      Objects.requireNonNull(parameterName);
      Objects.requireNonNull(link);
      skipNestedElements.put(parameterName, link);
      return this;
    }

    public SkipNodes build() {
      return new SkipNodes(skipMap, skipNestedElements);
    }
  }
}
