package org.opentripplanner.service.osminfo.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * TODO : Add java doc
 */
public class OsmWayReferences implements Serializable {

  private final List<String> references;

  public OsmWayReferences(Collection<String> references) {
    this.references = references.stream().sorted().distinct().toList();
  }

  /**
   * TODO : Add java doc
   *
   * returns a sorted distinct list of references
   */
  public List<String> references() {
    return references;
  }
}