package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.basic.Notice;

/**
 * Responsible for mapping onebusaway GTFS Notice into the OTP model.
 * Caches mapped instances so that the same GTFS notice produces the same OTP notice object.
 */
class NoticeMapper {

  private final IdFactory idFactory;
  private final Map<FeedScopedId, Notice> cache = new HashMap<>();

  NoticeMapper(IdFactory idFactory) {
    this.idFactory = idFactory;
  }

  Collection<Notice> map(Collection<org.onebusaway.gtfs.model.Notice> notices) {
    return notices.stream().map(this::map).toList();
  }

  Notice map(org.onebusaway.gtfs.model.Notice gtfsNotice) {
    var notice = Notice.of(idFactory.createId(gtfsNotice.getId(), "Notice"))
      .withText(gtfsNotice.getDisplayText())
      .build();
    return cache.computeIfAbsent(notice.getId(), _ -> notice);
  }

  Map<FeedScopedId, Notice> mappedNotices() {
    return Collections.unmodifiableMap(cache);
  }
}
