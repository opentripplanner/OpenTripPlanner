package org.opentripplanner.graph_builder.module.osm;

import java.time.Duration;
import java.util.Optional;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.api.Issue;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.street.model.edge.EscalatorEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;

/**
 * Contains the logic for extracting escalators out of OSM data
 */
class EscalatorProcessor {

  private final DataImportIssueStore issueStore;

  // If an escalator is tagged as moving less than 5 cm/s, or more than 5 m/s,
  // assume it's an error and ignore it.
  private static final double SLOW_ESCALATOR_ERROR_CUTOFF = 0.05;
  private static final double FAST_ESCALATOR_ERROR_CUTOFF = 5.0;

  public EscalatorProcessor(DataImportIssueStore issueStore) {
    this.issueStore = issueStore;
  }

  public EscalatorEdgePair buildEscalatorEdge(
    OsmWay escalatorWay,
    double length,
    IntersectionVertex fromVertex,
    IntersectionVertex toVertex
  ) {
    Optional<Duration> duration = escalatorWay.getDuration(v ->
      issueStore.add(
        Issue.issue(
          "InvalidDuration",
          "Duration for osm node {} is not a valid duration: '{}'; the value is ignored.",
          escalatorWay.url(),
          v
        )
      )
    );
    if (duration.isPresent()) {
      double speed = length / duration.get().toSeconds();
      if (speed < SLOW_ESCALATOR_ERROR_CUTOFF || speed > FAST_ESCALATOR_ERROR_CUTOFF) {
        duration = Optional.empty();
        issueStore.add(
          Issue.issue(
            "InvalidDuration",
            "Duration for osm node {} makes implied speed {} be outside acceptable range.",
            escalatorWay.url(),
            speed
          )
        );
      }
    }
    if (escalatorWay.isForwardEscalator()) {
      return new EscalatorEdgePair(
        EscalatorEdge.createEscalatorEdge(fromVertex, toVertex, length, duration.orElse(null)),
        null
      );
    }
    if (escalatorWay.isBackwardEscalator()) {
      return new EscalatorEdgePair(
        null,
        EscalatorEdge.createEscalatorEdge(toVertex, fromVertex, length, duration.orElse(null))
      );
    }
    return new EscalatorEdgePair(
      EscalatorEdge.createEscalatorEdge(fromVertex, toVertex, length, duration.orElse(null)),
      EscalatorEdge.createEscalatorEdge(toVertex, fromVertex, length, duration.orElse(null))
    );
  }
}
