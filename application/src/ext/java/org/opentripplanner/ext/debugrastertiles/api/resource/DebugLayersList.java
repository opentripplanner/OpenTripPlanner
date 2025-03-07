package org.opentripplanner.ext.debugrastertiles.api.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.opentripplanner.ext.debugrastertiles.TileRenderer;

/**
 * @author mabu
 */
public class DebugLayersList {

  public List<DebugLayer> layers;

  DebugLayersList(Map<String, TileRenderer> renderers) {
    layers = new ArrayList<>(renderers.size());
    for (Map.Entry<String, TileRenderer> layerInfo : renderers.entrySet()) {
      String layer_key = layerInfo.getKey();
      TileRenderer layer = layerInfo.getValue();
      layers.add(new DebugLayer(layer_key, layer.getName()));
    }
  }

  private static class DebugLayer {

    public String key;
    public String name;

    private DebugLayer(String layer_key, String layer_name) {
      this.key = layer_key;
      this.name = layer_name;
    }
  }
}
