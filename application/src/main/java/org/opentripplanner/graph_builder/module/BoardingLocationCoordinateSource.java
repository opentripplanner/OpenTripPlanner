package org.opentripplanner.graph_builder.module;

import org.opentripplanner.core.model.doc.DocumentedEnum;

/**
 * Selects which coordinate is used to place an {@link org.opentripplanner.street.model.vertex.OsmBoardingLocationVertex}
 * when a transit stop is linked to an OSM {@code boarding_location} platform (way or area).
 *
 * @see OsmBoardingLocationsModule
 */
public enum BoardingLocationCoordinateSource
  implements DocumentedEnum<BoardingLocationCoordinateSource>
{
  OSM(
    """
    Place the boarding location vertex at the centroid of the OSM platform. This is the historical
    behaviour: when several stops share a platform they collapse onto the same centroid vertex, which
    can produce unrealistically long on-platform transfers."""
  ),
  TRANSIT(
    """
    Place the boarding location vertex at the coordinate of the stop from the transit data. Each stop
    gets its own vertex, so two stops on the same platform become distinct, nearby vertices connected
    by a short path. The data provider controls the position, so a coordinate far from the mapped
    platform intentionally produces a non-trivial walk."""
  );

  private final String description;

  BoardingLocationCoordinateSource(String description) {
    this.description = description.stripIndent().trim();
  }

  @Override
  public String typeDescription() {
    return "Which coordinate is used to place the boarding location vertex when linking a stop to an OSM platform.";
  }

  @Override
  public String enumValueDescription() {
    return description;
  }
}
