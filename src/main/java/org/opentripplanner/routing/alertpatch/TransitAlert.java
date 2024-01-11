package org.opentripplanner.routing.alertpatch;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.TransitBuilder;

public class TransitAlert extends AbstractTransitEntity<TransitAlert, TransitAlertBuilder> {

  private final I18NString headerText;
  private final I18NString descriptionText;
  private final I18NString detailText;
  private final I18NString adviceText;
  // TODO OTP2 we wanted to merge the GTFS single alertUrl and the SIRI multiple URLs.
  //      However, GTFS URLs are one-per-language in a single object, and SIRI URLs are N objects with no translation.
  private final I18NString url;
  private final List<AlertUrl> siriUrls;
  //null means unknown
  private final String type;
  private final AlertSeverity severity;
  private final AlertCause cause;
  private final AlertEffect effect;
  //null means unknown
  private final Integer priority;
  private final ZonedDateTime creationTime;
  private final Integer version;
  private final ZonedDateTime updatedTime;
  private final String siriCodespace;
  private final Set<EntitySelector> entities;
  private final List<TimePeriod> timePeriods;

  TransitAlert(TransitAlertBuilder builder) {
    super(builder.getId());
    this.headerText = builder.headerText();
    this.descriptionText = builder.descriptionText();
    this.detailText = builder.detailText();
    this.adviceText = builder.adviceText();
    this.url = builder.url();
    this.siriUrls = List.copyOf(builder.siriUrls());
    this.type = builder.type();
    this.severity = builder.severity();
    this.cause = builder.cause();
    this.effect = builder.effect();
    this.priority = builder.priority();
    this.creationTime = builder.creationTime();
    this.version = builder.version();
    this.updatedTime = builder.updatedTime();
    this.siriCodespace = builder.siriCodespace();
    this.entities = Set.copyOf(builder.entities());
    this.timePeriods = List.copyOf(builder.timePeriods());
  }

  public static TransitAlertBuilder of(FeedScopedId id) {
    return new TransitAlertBuilder(id);
  }

  public Optional<I18NString> headerText() {
    return Optional.ofNullable(headerText);
  }

  public Optional<I18NString> descriptionText() {
    return Optional.ofNullable(descriptionText);
  }

  public I18NString detailText() {
    return detailText;
  }

  public I18NString adviceText() {
    return adviceText;
  }

  public Optional<I18NString> url() {
    return Optional.ofNullable(url);
  }

  public List<AlertUrl> siriUrls() {
    return siriUrls;
  }

  public String type() {
    return type;
  }

  /**
   * The severity of the alert.
   */
  public AlertSeverity severity() {
    return severity;
  }

  /**
   * The cause of the disruption.
   */
  public AlertCause cause() {
    return cause;
  }

  /**
   * The effect of the disruption.
   */
  public AlertEffect effect() {
    return effect;
  }

  public Integer priority() {
    return priority;
  }

  public ZonedDateTime creationTime() {
    return creationTime;
  }

  /**
   * Note: Only supported for TransitAlerts created from SIRI-SX messages
   *
   * @return Version as provided, or <code>null</code>
   */
  @Nullable
  public Integer version() {
    return version;
  }

  public ZonedDateTime updatedTime() {
    return updatedTime;
  }

  public String siriCodespace() {
    return siriCodespace;
  }

  public Set<EntitySelector> entities() {
    return entities;
  }

  public Collection<TimePeriod> timePeriods() {
    return timePeriods;
  }

  public boolean displayDuring(long startTimeSeconds, long endTimeSeconds) {
    for (TimePeriod timePeriod : timePeriods) {
      if (endTimeSeconds >= timePeriod.startTime) {
        if (timePeriod.endTime == 0 || startTimeSeconds < timePeriod.endTime) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Finds the first validity startTime from all timePeriods for this alert.
   *
   * @return First startDate for this Alert, <code>null</code> if 0 (not set)
   */
  @Nullable
  public Instant getEffectiveStartDate() {
    return timePeriods
      .stream()
      .map(timePeriod -> timePeriod.startTime)
      .min(Comparator.naturalOrder())
      .filter(startTime -> startTime > 0) //If 0, null should be returned
      .map(Instant::ofEpochSecond)
      .orElse(null);
  }

  /**
   * Finds the last validity endTime from all timePeriods for this alert. Returns <code>null</code>
   * if the validity is open-ended
   *
   * @return Last endDate for this Alert, <code>null</code> if open-ended
   */
  @Nullable
  public Instant getEffectiveEndDate() {
    return timePeriods
      .stream()
      .map(timePeriod -> timePeriod.endTime)
      .max(Comparator.naturalOrder())
      .filter(endTime -> endTime < TimePeriod.OPEN_ENDED) //If open-ended, null should be returned
      .map(Instant::ofEpochSecond)
      .orElse(null);
  }

  @Override
  public boolean sameAs(@Nonnull TransitAlert other) {
    return (
      getId().equals(other.getId()) &&
      Objects.equals(headerText, other.headerText) &&
      Objects.equals(descriptionText, other.descriptionText) &&
      Objects.equals(detailText, other.detailText) &&
      Objects.equals(adviceText, other.adviceText) &&
      Objects.equals(url, other.url) &&
      Objects.equals(siriUrls, other.siriUrls) &&
      Objects.equals(type, other.type) &&
      Objects.equals(severity, other.severity) &&
      Objects.equals(cause, other.cause) &&
      Objects.equals(effect, other.effect) &&
      Objects.equals(priority, other.priority) &&
      Objects.equals(creationTime, other.creationTime) &&
      Objects.equals(version, other.version) &&
      Objects.equals(updatedTime, other.updatedTime) &&
      Objects.equals(siriCodespace, other.siriCodespace) &&
      Objects.equals(entities, other.entities) &&
      Objects.equals(timePeriods, other.timePeriods)
    );
  }

  @Nonnull
  @Override
  public TransitBuilder<TransitAlert, TransitAlertBuilder> copy() {
    return new TransitAlertBuilder(this);
  }
}
