package org.opentripplanner.ext.gtfsgraphqlapi.model;

/**
 * Class for unknown entities. Either no entity was defined or an entity that we don't support yet
 * in that context.
 */
public class LegacyGraphQLUnknownModel {

  /**
   * Description of the entity
   */
  private final String description;

  public LegacyGraphQLUnknownModel(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
