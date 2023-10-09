package org.opentripplanner.apis.gtfs.model;

/**
 * Class for unknown entities. Either no entity was defined or an entity that we don't support yet
 * in that context.
 */
public class UnknownModel {

  /**
   * Description of the entity
   */
  private final String description;

  public UnknownModel(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
