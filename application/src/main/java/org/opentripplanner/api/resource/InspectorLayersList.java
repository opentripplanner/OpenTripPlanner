package org.opentripplanner.api.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.opentripplanner.inspector.raster.TileRenderer;

/**
 * @author mabu
 */
public class InspectorLayersList {

  public List<InspectorLayer> layers;

  InspectorLayersList(Map<String, TileRenderer> renderers) {
    layers = new ArrayList<>(renderers.size());
    for (Map.Entry<String, TileRenderer> layerInfo : renderers.entrySet()) {
      String layer_key = layerInfo.getKey();
      TileRenderer layer = layerInfo.getValue();
      layers.add(new InspectorLayer(layer_key, layer.getName()));
    }
  }

  private static class InspectorLayer {

    public String key;
    public String name;

    private InspectorLayer(String layer_key, String layer_name) {
      this.key = layer_key;
      this.name = layer_name;
    }
  }
}
