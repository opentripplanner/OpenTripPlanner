package org.opentripplanner.graph_builder.module;

import org.opentripplanner.core.model.doc.DocumentedEnum;

/**
 * Selects which coordinate is used to place an {@link org.opentripplanner.street.model.vertex.OsmBoardingLocationVertex}
 * when a transit stop is linked to an OSM {@code boarding_location} (way, area or node).
 *
 * @see OsmBoardingLocationsModule
 */
public enum BoardingLocationCoordinateSource
  implements DocumentedEnum<BoardingLocationCoordinateSource>
{
  OSM(
    """
    Use the position from the OSM data. This is the historical behaviour: every stop matching one
    platform shares a single point at its centre, so walking between them is free however far apart
    they really are, and every walk to or from the platform starts at that centre rather than at the
    stop."""
  ),
  TRANSIT(
    """
    Use the position from the transit data. Each stop keeps its own coordinate and is connected to
    the platform by a walk of the real distance, so stops on one platform stay separate. The OSM
    features themselves are never moved."""
  );

  private final String description;

  BoardingLocationCoordinateSource(String description) {
    this.description = description.stripIndent().trim();
  }

  @Override
  public String typeDescription() {
    return "Which position OTP uses for a stop that matches an OSM platform or boarding location node.";
  }

  @Override
  public String enumValueDescription() {
    return description;
  }
}
