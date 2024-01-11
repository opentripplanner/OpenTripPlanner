package org.opentripplanner.raptor.api.path;

import java.time.ZonedDateTime;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorValueFormatter;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;

/**
 * Create a path like: {@code Walk 5m - 101 - Transit 10:07 10:35 - 2111 - Walk 4m }
 */
@SuppressWarnings("UnusedReturnValue")
public class PathStringBuilder {

  private final RaptorStopNameResolver stopNameResolver;
  private final StringBuilder buf = new StringBuilder();

  private boolean addPadding = false;

  /**
   * @param stopNameResolver Used to translate stopIndexes to stopNames, if {@code null} the index
   *                         is used in the result string.
   */
  public PathStringBuilder(@Nullable RaptorStopNameResolver stopNameResolver) {
    this.stopNameResolver = RaptorStopNameResolver.nullSafe(stopNameResolver);
  }

  /**
   * The given {@code stopIndex} is translated to stop name using the {@code stopNameTranslator} set
   * in the constructor. If not translator is set the stopIndex is used.
   */
  public PathStringBuilder stop(int stopIndex) {
    return stop(stopNameResolver.apply(stopIndex));
  }

  public PathStringBuilder stop(String stop) {
    return legSep().text(stop);
  }

  public PathStringBuilder walk(int duration) {
    return legSep().text("Walk").duration(duration);
  }

  public PathStringBuilder pickupRental(String stop, int duration) {
    return legSep().text(stop).text("Rental").duration(duration);
  }

  public PathStringBuilder accessEgress(RaptorAccessEgress leg) {
    if (leg.isFree()) {
      return this;
    }
    return legSep().text(leg.asString(false, false, null));
  }

  public PathStringBuilder transit(String description, int fromTime, int toTime) {
    return legSep().text(description).time(fromTime, toTime);
  }

  public PathStringBuilder transit(
    String modeName,
    String trip,
    ZonedDateTime fromTime,
    ZonedDateTime toTime
  ) {
    return legSep().text(modeName).text(trip).time(fromTime, toTime);
  }

  public PathStringBuilder street(String modeName, ZonedDateTime fromTime, ZonedDateTime toTime) {
    return legSep().text(modeName).time(fromTime, toTime);
  }

  public PathStringBuilder c1(int c1) {
    if (c1 == RaptorCostCalculator.ZERO_COST) {
      return this;
    }
    return text(RaptorValueFormatter.formatC1(c1));
  }

  public PathStringBuilder c2(int c2) {
    if (c2 == RaptorConstants.NOT_SET) {
      return this;
    }
    return text(RaptorValueFormatter.formatC2(c2));
  }

  public PathStringBuilder waitTimeCost(int wtc, int defaultValue) {
    if (wtc == defaultValue) {
      return this;
    }
    return text(RaptorValueFormatter.formatWaitTimeCost(wtc));
  }

  public PathStringBuilder transferPriority(int transferPriorityCost, int defaultValue) {
    if (transferPriorityCost == defaultValue) {
      return this;
    }
    return text(RaptorValueFormatter.formatTransferPriority(transferPriorityCost));
  }

  public PathStringBuilder duration(int duration) {
    return text(DurationUtils.durationToStr(duration));
  }

  public PathStringBuilder time(int from, int to) {
    return time(from).time(to);
  }

  public PathStringBuilder time(int time) {
    return time != RaptorConstants.TIME_NOT_SET ? text(TimeUtils.timeToStrCompact(time)) : this;
  }

  public PathStringBuilder numberOfTransfers(int nTransfers) {
    return nTransfers != RaptorConstants.NOT_SET
      ? text(RaptorValueFormatter.formatNumOfTransfers(nTransfers))
      : this;
  }

  public PathStringBuilder summary(int c1, int c2) {
    return summaryStart().c1(c1).c2(c2).summaryEnd();
  }

  public PathStringBuilder summary(int startTime, int endTime, int nTransfers, int c1, int c2) {
    return summary(startTime, endTime, nTransfers, c1, c2, null);
  }

  public PathStringBuilder summary(
    int startTime,
    int endTime,
    int nTransfers,
    int c1,
    int c2,
    @Nullable Consumer<PathStringBuilder> appendToSummary
  ) {
    summaryStart()
      .time(startTime, endTime)
      .duration(Math.abs(endTime - startTime))
      .numberOfTransfers(nTransfers)
      .c1(c1)
      .c2(c2);

    if (appendToSummary != null) {
      appendToSummary.accept(this);
    }
    return summaryEnd();
  }

  @Override
  public String toString() {
    return buf.toString();
  }

  /* private helpers */

  private PathStringBuilder summaryStart() {
    text("[");
    addPadding = false;
    return this;
  }

  private PathStringBuilder summaryEnd() {
    buf.append(']');
    return this;
  }

  private PathStringBuilder time(ZonedDateTime from, ZonedDateTime to) {
    return text(TimeUtils.timeToStrCompact(from)).text(TimeUtils.timeToStrCompact(to));
  }

  private PathStringBuilder legSep() {
    if (addPadding) {
      text("~");
    }
    return this;
  }

  private PathStringBuilder sep() {
    if (addPadding) {
      buf.append(' ');
    }
    return this;
  }

  public PathStringBuilder text(String text) {
    sep();
    buf.append(text);
    addPadding = true;
    return this;
  }
}
