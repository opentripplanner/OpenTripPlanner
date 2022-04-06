package org.opentripplanner.routing.framework;

import io.micrometer.core.instrument.Tag;
import java.util.Collection;
import java.util.List;

public class MicrometerUtils {

  public static List<Tag> mapTimingTags(Collection<String> timingTags) {
    return List.of(Tag.of("tags", String.join(" ", timingTags)));
  }
}
