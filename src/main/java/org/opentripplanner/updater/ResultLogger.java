package org.opentripplanner.updater;

import java.util.stream.Collectors;
import org.opentripplanner.framework.lang.DoubleUtils;
import org.opentripplanner.model.UpdateError;
import org.opentripplanner.updater.trip.UpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs a nicely formatted summary of the result of a realtime update which is very helpful
 * for debugging.
 */
public class ResultLogger {

  private static final Logger LOG = LoggerFactory.getLogger(ResultLogger.class);

  public static void logUpdateResult(
    String feedId,
    String type,
    int totalUpdates,
    UpdateResult updateResult
  ) {
    LOG.info(
      "[feedId: {}, type={}] {} of {} update messages were applied successfully (success rate: {}%)",
      feedId,
      type,
      updateResult.successful(),
      totalUpdates,
      DoubleUtils.roundTo2Decimals((double) updateResult.successful() / totalUpdates * 100)
    );

    var errorIndex = updateResult.failures();

    errorIndex
      .keySet()
      .forEach(key -> {
        var value = errorIndex.get(key);
        var tripIds = value.stream().map(UpdateError::debugId).collect(Collectors.toSet());
        LOG.error(
          "[feedId: {}, type={}] {} failures of type {}: {}",
          feedId,
          type,
          value.size(),
          key,
          tripIds
        );
      });
  }
}
