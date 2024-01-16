package org.opentripplanner.framework.time;

import java.time.ZoneId;
import javax.annotation.Nullable;
import org.opentripplanner.framework.logging.Throttle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides a fallback mechanism for retrieving a zone id (=time zone).
 * If a ZoneId is not provided, it returns a default fallback ZoneId (UTC).
 * <p>
 * This situation happens when you don't load any transit data into the graph but want to route
 * anyway, perhaps only on the street network.
 */
public class ZoneIdFallback {

  private static final Logger LOG = LoggerFactory.getLogger(ZoneIdFallback.class);
  private static final ZoneId FALLBACK = ZoneId.of("UTC");
  private static final Throttle THROTTLE = Throttle.ofOneMinute();

  /**
   * Accepts a nullable zone id (time zone) and returns UTC as the fallback.
   */
  public static ZoneId zoneId(@Nullable ZoneId id) {
    if (id == null) {
      THROTTLE.throttle(() -> {
        LOG.warn(
          "Your instance doesn't contain a time zone (which is usually derived from transit data). Assuming {}.",
          FALLBACK
        );
        LOG.warn("Please double-check that transit data was correctly loaded.");
      });
      return FALLBACK;
    } else {
      return id;
    }
  }
}
