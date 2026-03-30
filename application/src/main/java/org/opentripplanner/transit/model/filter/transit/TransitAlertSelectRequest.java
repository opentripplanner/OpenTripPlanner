package org.opentripplanner.transit.model.filter.transit;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.time.TimePeriod;
import org.opentripplanner.routing.alertpatch.AlertCause;
import org.opentripplanner.routing.alertpatch.AlertEffect;
import org.opentripplanner.routing.alertpatch.AlertSeverity;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Represents a single selection criterion for filtering {@link TransitAlert}s.
 * Criteria within a single request are combined with AND logic: all specified criteria must
 * match for the request to match. Unset (null) criteria are ignored (match everything).
 */
public class TransitAlertSelectRequest {

  private final FilterValues<String> feeds;
  private final FilterValues<AlertSeverity> severityLevels;
  private final FilterValues<AlertCause> causes;
  private final FilterValues<AlertEffect> effects;
  private final FilterValues<TimePeriod> timePeriods;

  private TransitAlertSelectRequest(Builder builder) {
    this.feeds = FilterValues.ofNullIsEverything("feeds", builder.feeds);
    this.severityLevels = FilterValues.ofNullIsEverything("severityLevels", builder.severityLevels);
    this.causes = FilterValues.ofNullIsEverything("causes", builder.causes);
    this.effects = FilterValues.ofNullIsEverything("effects", builder.effects);
    this.timePeriods = FilterValues.ofNullIsEverything("timePeriods", builder.timePeriods);
  }

  public static Builder of() {
    return new Builder();
  }

  public FilterValues<String> feeds() {
    return feeds;
  }

  public FilterValues<AlertSeverity> severityLevels() {
    return severityLevels;
  }

  public FilterValues<AlertCause> causes() {
    return causes;
  }

  public FilterValues<AlertEffect> effects() {
    return effects;
  }

  public FilterValues<TimePeriod> timePeriods() {
    return timePeriods;
  }

  @Override
  public String toString() {
    var builder = ToStringBuilder.ofEmbeddedType();
    if (!feeds.includeEverything()) {
      builder.addCol("feeds", feeds.get());
    }
    if (!severityLevels.includeEverything()) {
      builder.addCol("severityLevels", severityLevels.get());
    }
    if (!causes.includeEverything()) {
      builder.addCol("causes", causes.get());
    }
    if (!effects.includeEverything()) {
      builder.addCol("effects", effects.get());
    }
    if (!timePeriods.includeEverything()) {
      builder.addCol("timePeriods", timePeriods.get());
    }
    return builder.toString();
  }

  public static class Builder {

    @Nullable
    private List<String> feeds;

    @Nullable
    private List<AlertSeverity> severityLevels;

    @Nullable
    private List<AlertCause> causes;

    @Nullable
    private List<AlertEffect> effects;

    @Nullable
    private List<TimePeriod> timePeriods;

    public Builder withFeeds(@Nullable List<String> feeds) {
      this.feeds = feeds;
      return this;
    }

    public Builder withSeverityLevels(@Nullable List<AlertSeverity> severityLevels) {
      this.severityLevels = severityLevels;
      return this;
    }

    public Builder withCauses(@Nullable List<AlertCause> causes) {
      this.causes = causes;
      return this;
    }

    public Builder withEffects(@Nullable List<AlertEffect> effects) {
      this.effects = effects;
      return this;
    }

    public Builder withTimePeriods(@Nullable List<TimePeriod> timePeriods) {
      this.timePeriods = timePeriods;
      return this;
    }

    /**
     * Returns true if no criterion is set, in which case the selector would match everything.
     */
    public boolean isEmpty() {
      return (
        feeds == null &&
        severityLevels == null &&
        causes == null &&
        effects == null &&
        timePeriods == null
      );
    }

    public TransitAlertSelectRequest build() {
      return new TransitAlertSelectRequest(this);
    }
  }
}
