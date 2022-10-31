package org.opentripplanner.generate.doc.framework;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.opentripplanner.standalone.config.framework.json.NodeInfo;

/**
 * This is a simple implementation of the {@link SkipFunction}. It keeps a map from
 * {@link NodeInfo#name()}s to a url link (String). If the {@link NodeInfo#name()} exist in the
 * map, we skip it.
 */
public class SkipNodes implements SkipFunction {

  private final Map<String, String> map;

  public SkipNodes(Map<String, String> map) {
    this.map = Map.copyOf(map);
  }

  public static Builder of() {
    return new Builder();
  }

  @Override
  public boolean skip(NodeInfo node) {
    return map.containsKey(node.name());
  }

  @Override
  public Optional<String> linkToDoc(NodeInfo node) {
    return Optional.ofNullable(map.get(node.name()));
  }

  private void put(String key, String value) {
    map.put(key, value);
  }

  public static class Builder {

    Map<String, String> map = new HashMap<>();

    public Builder add(String parameterName, String link) {
      map.put(parameterName, link);
      return this;
    }

    public SkipNodes build() {
      return new SkipNodes(map);
    }
  }
}
