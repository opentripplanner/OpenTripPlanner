package org.opentripplanner.ext.geocoder;

import java.util.Collection;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * A package-private helper type for transporting data before serializing.
 */
record LuceneStopCluster(
  FeedScopedId id,
  @Nullable String code,
  String name,
  StopCluster.Coordinate coordinate,
  Collection<String> modes,
  Collection<String> agencyIds
) {}
