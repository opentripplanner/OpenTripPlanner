package org.opentripplanner.raptor.api.path;

import java.time.ZonedDateTime;
import javax.annotation.Nullable;
import org.opentripplanner.framework.lang.OtpNumberFormat;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.raptor.spi.CostCalculator;
import org.opentripplanner.raptor.spi.RaptorAccessEgress;
import org.opentripplanner.raptor.spi.RaptorStopNameResolver;

/**
 * Create a path like: {@code Walk 5m - 101 - Transit 10:07 10:35 - 2111 - Walk 4m }
 */
@SuppressWarnings("UnusedReturnValue")
public class PathStringBuilder {

  private final RaptorStopNameResolver stopNameResolver;
  private final StringBuilder buf = new StringBuilder();
  private final boolean padDuration;
  private boolean elementAdded = false;
  private boolean sepAdded = false;

  public PathStringBuilder(@Nullable RaptorStopNameResolver stopNameResolver) {
    this(stopNameResolver, false);
  }

  /**
   * @param stopNameResolver Used to translate stopIndexes to stopNames, if {@code null} the index
   *                         is used in the result string.
   * @param padDuration      This can be set to {@code true} for padding the duration output. This
   *                         would be used in cases were several similar paths are listed. If the
   *                         legs are similar, the path elements is more likely to be aligned.
   */
  public PathStringBuilder(@Nullable RaptorStopNameResolver stopNameResolver, boolean padDuration) {
    this.stopNameResolver = RaptorStopNameResolver.nullSafe(stopNameResolver);
    this.padDuration = padDuration;
  }

  public PathStringBuilder sep() {
    sepAdded = true;
    return this;
  }

  /**
   * The given {@code stopIndex} is translated to stop name using the {@code stopNameTranslator} set
   * in the constructor. If not translator is set the stopIndex is used.
   */
  public PathStringBuilder stop(int stopIndex) {
    return stop(stopNameResolver.apply(stopIndex));
  }

  public PathStringBuilder stop(String stop) {
    return start().append(stop).end();
  }

  public PathStringBuilder walk(int duration) {
    return start().append("Walk").duration(duration).end();
  }

  public PathStringBuilder rental(int duration) {
    return start().append("Rental").space().duration(duration).end();
  }

  public PathStringBuilder flex(int duration, int nRides) {
    // The 'tx' is short for eXtra Transfers added by the flex access/egress.
    return start().append("Flex").duration(duration).space().append(nRides).append("x").end();
  }

  public PathStringBuilder accessEgress(RaptorAccessEgress leg) {
    if (leg.hasRides()) {
      return flex(leg.durationInSeconds(), leg.numberOfRides());
    }
    return leg.durationInSeconds() == 0 ? this : walk(leg.durationInSeconds());
  }

  public PathStringBuilder transit(String description, int fromTime, int toTime) {
    return start().append(description).space().time(fromTime, toTime).end();
  }

  public PathStringBuilder transit(
    String modeName,
    String trip,
    ZonedDateTime fromTime,
    ZonedDateTime toTime
  ) {
    return start().append(modeName).space().append(trip).space().time(fromTime, toTime).end();
  }

  public PathStringBuilder street(String modeName, ZonedDateTime fromTime, ZonedDateTime toTime) {
    return start().append(modeName).space().time(fromTime, toTime).end();
  }

  public PathStringBuilder timeAndCostCentiSec(int fromTime, int toTime, int generalizedCost) {
    if (buf.length() != 0) {
      space();
    }
    return time(fromTime, toTime).generalizedCostSentiSec(generalizedCost);
  }

  /** Add generalizedCostCentiSec {@link #costCentiSec(int, int, String)} */
  public PathStringBuilder generalizedCostSentiSec(int cost) {
    return costCentiSec(cost, CostCalculator.ZERO_COST, null);
  }

  /**
   * Add a cost to the string with an optional unit. Try to be consistent with unit naming, use
   * lower-case:
   * <ul>
   *     <li>{@code null} - Generalized-cost (no unit used)</li>
   *     <li>{@code "wtc"} - Wait-time cost</li>
   *     <li>{@code "pri"} - Transfer priority cost</li>
   * </ul>
   */
  public PathStringBuilder costCentiSec(int cost, int defaultValue, String unit) {
    if (cost == defaultValue) {
      return this;
    }
    space().append(OtpNumberFormat.formatCostCenti(cost));
    if (unit != null) {
      append(unit);
    }
    return this;
  }

  public PathStringBuilder space() {
    return append(" ");
  }

  public PathStringBuilder duration(int duration) {
    String durationStr = DurationUtils.durationToStr(duration);
    return space().append(padDuration ? String.format("%5s", durationStr) : durationStr);
  }

  public PathStringBuilder time(int from, int to) {
    return append(TimeUtils.timeToStrCompact(from)).space().append(TimeUtils.timeToStrCompact(to));
  }

  public PathStringBuilder append(String text) {
    buf.append(text);
    return this;
  }

  @Override
  public String toString() {
    return buf.toString();
  }

  /* private helpers */

  private PathStringBuilder time(ZonedDateTime from, ZonedDateTime to) {
    return append(TimeUtils.timeToStrCompact(from)).space().append(TimeUtils.timeToStrCompact(to));
  }

  private PathStringBuilder append(int value) {
    buf.append(value);
    return this;
  }

  private PathStringBuilder end() {
    elementAdded = true;
    sepAdded = false;
    return this;
  }

  private PathStringBuilder start() {
    if (sepAdded && elementAdded) {
      append(" ~ ");
    }
    elementAdded = false;
    return this;
  }
}
