package org.opentripplanner.gtfs.mapping;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Objects;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.fares.model.Timeframe;

class TimeframeMapper {

  private final IdFactory idFactory;
  private final Multimap<FeedScopedId, Timeframe> mappedTimeframes = ArrayListMultimap.create();

  public TimeframeMapper(IdFactory idFactory) {
    this.idFactory = idFactory;
  }

  public Timeframe map(org.onebusaway.gtfs.model.Timeframe rhs) {
    final FeedScopedId serviceId = idFactory.createId(
      rhs.getServiceId(),
      "fare leg rule's timeframe"
    );
    var t = Timeframe.of()
      .withServiceId(serviceId)
      .withStart(Objects.requireNonNullElse(rhs.getStartTime(), LocalTime.MIN))
      // LocalTime.MAX is 23:59.9999999
      .withEnd(Objects.requireNonNullElse(rhs.getEndTime(), LocalTime.MAX))
      .build();
    var groupId = idFactory.createId(rhs.getTimeframeGroupId(), "timeframe's group id");
    mappedTimeframes.put(groupId, t);
    return t;
  }

  public Collection<Timeframe> map(Collection<org.onebusaway.gtfs.model.Timeframe> timeframes) {
    return timeframes.stream().map(this::map).toList();
  }

  /**
   * Return the timeframes for a given timeframe group id.
   */
  public Collection<Timeframe> findTimeframes(FeedScopedId groupId) {
    return mappedTimeframes.get(groupId);
  }
}
