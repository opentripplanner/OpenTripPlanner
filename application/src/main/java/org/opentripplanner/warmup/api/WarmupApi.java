package org.opentripplanner.warmup.api;

import org.opentripplanner.core.model.doc.DocumentedEnum;

/** Which GraphQL API to use for warmup queries. */
public enum WarmupApi implements DocumentedEnum<WarmupApi> {
  TRANSMODEL("Use the TransModel GraphQL API for warmup queries."),
  GTFS("Use the GTFS GraphQL API for warmup queries.");

  private final String description;

  WarmupApi(String description) {
    this.description = description;
  }

  @Override
  public String typeDescription() {
    return "Which GraphQL API to use for warmup queries.";
  }

  @Override
  public String enumValueDescription() {
    return description;
  }
}
