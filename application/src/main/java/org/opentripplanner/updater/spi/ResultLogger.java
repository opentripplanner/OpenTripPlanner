package org.opentripplanner.updater.spi;

import static net.logstash.logback.argument.StructuredArguments.keyValue;

import java.util.stream.Collectors;
import org.opentripplanner.utils.lang.DoubleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs a nicely formatted summary of the result of a realtime update which is very helpful
 * for debugging.
 */
public class ResultLogger {

  private static final Logger LOG = LoggerFactory.getLogger(ResultLogger.class);

  public static void logUpdateResult(String feedId, String type, UpdateResult updateResult) {
    var totalUpdates = updateResult.successful() + updateResult.failed();
    if (totalUpdates > 0) {
      LOG.info(
        "[feedId={}, type={}] {} of {} update messages were applied successfully (success rate: {}%)",
        feedId,
        type,
        updateResult.successful(),
        totalUpdates,
        DoubleUtils.roundTo2Decimals(((double) updateResult.successful() / totalUpdates) * 100)
      );

      logUpdateResultErrors(feedId, type, updateResult);
    } else {
      LOG.info("[feedId={}, type={}] Feed did not contain any updates", feedId, type);
    }
  }

  public static void logUpdateResultErrors(String feedId, String type, UpdateResult updateResult) {
    if (updateResult.failed() == 0) {
      return;
    }
    var errorIndex = updateResult.failures();
    errorIndex
      .keySet()
      .forEach(key -> {
        var value = errorIndex.get(key);
        var tripIds = value.stream().map(UpdateError::debugId).collect(Collectors.toSet());
        LOG.warn(
          "[{} {}] {} failures of {}: {}",
          keyValue("feedId", feedId),
          keyValue("type", type),
          value.size(),
          keyValue("errorType", key),
          tripIds
        );
      });
  }
}
