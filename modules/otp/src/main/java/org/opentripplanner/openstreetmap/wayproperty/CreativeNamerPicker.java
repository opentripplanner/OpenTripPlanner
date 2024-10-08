package org.opentripplanner.openstreetmap.wayproperty;

import org.opentripplanner.openstreetmap.wayproperty.specifier.OsmSpecifier;

/**
 * Describes how unnamed OSM ways are to be named based on the tags they possess. The CreativeNamer
 * will be applied to ways that match the OSMSpecifier.
 *
 * @author novalis
 */
public class CreativeNamerPicker {

  public OsmSpecifier specifier;
  public CreativeNamer namer;

  public CreativeNamerPicker() {
    specifier = null;
    namer = null;
  }

  public CreativeNamerPicker(OsmSpecifier specifier, CreativeNamer namer) {
    this.specifier = specifier;
    this.namer = namer;
  }
}
