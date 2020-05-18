package org.opentripplanner.standalone.config.updater_config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.opentripplanner.standalone.config.NodeAdapter;

import java.util.ArrayList;
import java.util.List;

public class UpdaterConfig {

  public static final UpdaterConfig DEFAULT = new UpdaterConfig(
      MissingNode.getInstance()
  );

  private final List<UpdaterConfigItem> configItems = new ArrayList<>();

  public UpdaterConfig(JsonNode c) {
    for (JsonNode updater : c) {
      configItems.add(new UpdaterConfigItem(new NodeAdapter(updater, null)));
    }
  }

  public List<UpdaterConfigItem> getItems() {
    return configItems;
  }
}
