package org.opentripplanner.ext.empiricaldelay.model;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/** Utility class to store trip-time delays for a given service id. */
public class TripDelays implements Serializable {

  private final FeedScopedId tripId;
  private Map<String, TIntObjectMap<EmpiricalDelay>> delaysByServiceId = new HashMap<>();

  public TripDelays(
    FeedScopedId tripId,
    Map<String, TIntObjectMap<EmpiricalDelay>> delaysByServiceId
  ) {
    this.tripId = Objects.requireNonNull(tripId);
    this.delaysByServiceId = Objects.requireNonNull(delaysByServiceId);
  }

  public static TripDelays.Builder of(FeedScopedId tripId) {
    return new Builder(tripId);
  }

  public FeedScopedId tripId() {
    return tripId;
  }

  public Optional<EmpiricalDelay> get(String serviceId, int stopPosInPattern) {
    TIntObjectMap<EmpiricalDelay> delayPerStop = delaysByServiceId.get(serviceId);
    // Check if empirical data for the serviceId (serviceDay) exist
    if (delayPerStop == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(delayPerStop.get(stopPosInPattern));
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TripDelays.class)
      .addObj("tripId", tripId)
      .addObj("serviceIds", delaysByServiceId.keySet().stream().sorted().toList())
      .toString();
  }

  public static class Builder {

    private final FeedScopedId tripId;
    private Map<String, TIntObjectMap<EmpiricalDelay>> delaysByServiceId = new HashMap<>();

    public Builder(FeedScopedId tripId) {
      this.tripId = tripId;
    }

    public Builder with(String serviceId, TIntObjectMap<EmpiricalDelay> dalaysForEachStop) {
      delaysByServiceId.put(serviceId, dalaysForEachStop);
      return this;
    }

    /** Utility method for inserting delays for ALL stops in a pattern */
    public Builder with(String serviceId, List<EmpiricalDelay> delaysForEachStop) {
      var map = new TIntObjectHashMap<EmpiricalDelay>();
      for (int i = 0; i < delaysForEachStop.size(); i++) {
        map.put(i, delaysForEachStop.get(i));
      }
      with(serviceId, map);
      return this;
    }

    @Nullable
    public TripDelays build() {
      return delaysByServiceId.isEmpty() ? null : new TripDelays(tripId, delaysByServiceId);
    }
  }
}
