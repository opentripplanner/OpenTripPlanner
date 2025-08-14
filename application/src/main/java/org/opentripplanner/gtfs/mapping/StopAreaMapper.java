package org.opentripplanner.gtfs.mapping;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import org.onebusaway.gtfs.model.StopAreaElement;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class StopAreaMapper {

  private final IdFactory idFactory;

  StopAreaMapper(IdFactory idFactory) {
    this.idFactory = idFactory;
  }

  public Multimap<FeedScopedId, FeedScopedId> map(Collection<StopAreaElement> entries) {
    var res = ArrayListMultimap.<FeedScopedId, FeedScopedId>create();
    entries.forEach(e -> {
      var stopId = idFactory.createId(e.getStop().getId(), "stop area assignment");
      var areaId = idFactory.createId(e.getArea().getId(), "stop area assignment");
      res.put(stopId, areaId);
    });
    return res;
  }
}
