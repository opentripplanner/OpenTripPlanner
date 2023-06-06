package org.opentripplanner.raptor.api.path;

import java.time.ZonedDateTime;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.opentripplanner.framework.lang.OtpNumberFormat;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorConstants;
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
    return legSep().text(leg.asString(false));
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

  public PathStringBuilder timeAndCostCentiSec(int fromTime, int toTime, int generalizedCost) {
    return time(fromTime, toTime).generalizedCostSentiSec(generalizedCost);
  }

  /** Add generalizedCostCentiSec {@link #costCentiSec(int, int, String)} */
  public PathStringBuilder generalizedCostSentiSec(int cost) {
    return costCentiSec(cost, RaptorCostCalculator.ZERO_COST, null);
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
  public PathStringBuilder costCentiSec(int generalizedCostCents, int defaultValue, String unit) {
    if (generalizedCostCents == defaultValue) {
      return this;
    }
    var costText = OtpNumberFormat.formatCostCenti(generalizedCostCents);
    return (unit != null) ? text(costText + unit) : text(costText);
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
    return nTransfers != RaptorConstants.NOT_SET ? text(nTransfers + "tx") : this;
  }

  public PathStringBuilder summary(int generalizedCostCents) {
    return summaryStart().generalizedCostSentiSec(generalizedCostCents).summaryEnd();
  }

  public PathStringBuilder summary(
    int startTime,
    int endTime,
    int nTransfers,
    int generalizedCostCents
  ) {
    return summary(startTime, endTime, nTransfers, generalizedCostCents, null);
  }

  public PathStringBuilder summary(
    int startTime,
    int endTime,
    int nTransfers,
    int generalizedCostCents,
    @Nullable Consumer<PathStringBuilder> appendToSummary
  ) {
    summaryStart()
      .time(startTime, endTime)
      .duration(Math.abs(endTime - startTime))
      .numberOfTransfers(nTransfers)
      .generalizedCostSentiSec(generalizedCostCents);
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
