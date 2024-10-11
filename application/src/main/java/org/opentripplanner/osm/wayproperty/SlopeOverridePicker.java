package org.opentripplanner.osm.wayproperty;

import org.opentripplanner.osm.wayproperty.specifier.OsmSpecifier;

public class SlopeOverridePicker {

  private OsmSpecifier specifier;

  private boolean override;

  public SlopeOverridePicker() {}

  public SlopeOverridePicker(OsmSpecifier specifier, boolean override) {
    this.specifier = specifier;
    this.override = override;
  }

  public OsmSpecifier getSpecifier() {
    return specifier;
  }

  public void setSpecifier(OsmSpecifier specifier) {
    this.specifier = specifier;
  }

  public boolean getOverride() {
    return override;
  }

  public void setOverride(boolean override) {
    this.override = override;
  }
}
