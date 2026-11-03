package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_11;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;

import java.util.List;
import java.util.Set;
import org.opentripplanner.graph_builder.module.BoardingLocationCoordinateSource;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * How transit stops are matched to the OSM features passengers wait at, and which of the two
 * positions a matched stop takes ({@code boardingLocations} section of {@code build-config.json}).
 */
public record BoardingLocationsConfig(
  Set<String> refTags,
  BoardingLocationCoordinateSource coordinateSource
) {
  public static BoardingLocationsConfig fromConfig(NodeAdapter root) {
    return fromSubConfig(
      root
        .of("boardingLocations")
        .since(V2_11)
        .summary("How transit stops are matched to OSM boarding locations.")
        .description("[Detailed documentation](BoardingLocations.md)")
        .asObject()
    );
  }

  public static BoardingLocationsConfig fromSubConfig(NodeAdapter c) {
    var refTags = c
      .of("refTags")
      .since(V2_2)
      .summary(
        "What OSM tags should be looked on for the source of matching stops to platforms and stops."
      )
      .description("[Detailed documentation](BoardingLocations.md)")
      .asStringSet(List.copyOf(Set.of("ref")));

    var coordinateSource = c
      .of("coordinateSource")
      .since(V2_11)
      .summary(
        "Which position OTP uses for a stop that matches an OSM platform or boarding location node."
      )
      .description(
        """
        When a stop's reference tag matches an OSM platform (a way or an area) or a tagged node, OTP
        has two candidate positions for where the passenger waits: the one in the OSM data and the
        one in the transit data.

        The default `OSM` uses the OSM feature. Every stop matching one platform shares a single
        point at its centre, so walking between them is free however far apart they really are, and
        every walk to or from the platform starts at that centre rather than at the stop — on a long
        platform, up to half its length of detour.

        `TRANSIT` keeps each stop where the transit data puts it and connects it to the platform by
        a walk of the real distance, so stops on one platform stay separate. This applies to
        platforms mapped as ways or areas and to stops matching a tagged node alike; the OSM
        features themselves are never moved. A gap too large to be a surveying discrepancy is
        reported as a data import issue.
        """
      )
      .asEnum(BoardingLocationCoordinateSource.OSM);

    return new BoardingLocationsConfig(refTags, coordinateSource);
  }
}
