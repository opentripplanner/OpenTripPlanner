package org.opentripplanner.routing.framework;

import io.micrometer.core.instrument.Tag;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.routing.api.request.RoutingTag;

public class MicrometerUtils {

  public static List<Tag> mapTimingTags(Collection<RoutingTag> tags) {
    return tags
      .stream()
      .filter(RoutingTag::includeInMicrometerTiming)
      .map(t -> Tag.of(t.getCategory().name(), t.getTag()))
      .toList();
  }
}
