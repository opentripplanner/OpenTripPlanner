package org.opentripplanner.osm.wayproperty;

import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.osm.model.OsmEntity;

/**
 * A CreativeNamer makes up names for ways that don't have one in the OSM data set. It does this by
 * substituting the values of OSM tags into a template.
 */
public class CreativeNamer {

  /**
   * A creative name pattern is a template which may contain variables of the form {{tag_name}}.
   * When a way's creative name is created, the value of its tag tag_name is substituted for the
   * variable.
   * <p>
   * For example, "Highway with surface {{surface}}" might become "Highway with surface gravel"
   */
  private final String creativeNamePattern;

  public CreativeNamer(String pattern) {
    this.creativeNamePattern = pattern;
  }

  public I18NString generateCreativeName(OsmEntity way) {
    return LocalizedStringMapper.getInstance().map(creativeNamePattern, way);
  }
}
