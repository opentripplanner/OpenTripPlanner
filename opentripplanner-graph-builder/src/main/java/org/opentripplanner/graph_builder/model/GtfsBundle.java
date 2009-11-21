package org.opentripplanner.graph_builder.model;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class GtfsBundle {

  private File path;

  private String defaultAgencyId;

  private Map<String, String> agencyIdMappings = new HashMap<String, String>();

  public File getPath() {
    return path;
  }

  public void setPath(File path) {
    this.path = path;
  }

  public String getDefaultAgencyId() {
    return defaultAgencyId;
  }

  public void setDefaultAgencyId(String defaultAgencyId) {
    this.defaultAgencyId = defaultAgencyId;
  }

  public Map<String, String> getAgencyIdMappings() {
    return agencyIdMappings;
  }

  public void setAgencyIdMappings(Map<String, String> agencyIdMappings) {
    this.agencyIdMappings = agencyIdMappings;
  }
}
