package org.opentripplanner.routing.alertpatch;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.core.model.time.TimePeriod;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.TransitBuilder;

/**
 * Internal representation of a GTFS-RT Service Alert or SIRI Situation Exchange (SX) message.
 * These are text descriptions of problems affecting specific stops, routes, or other components
 * of the transit system which will be displayed to users as text.
 * Although they have flags describing the effect of the problem described in the text, these
 * messages do not currently modify routing behavior on their own. They must be accompanied by
 * messages of other types to actually impact routing. However, there is ongoing discussion about
 * allowing Alerts to affect routing, especially for cases such as stop closure messages.
 */
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
  private final AlertCalendar calendar;

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
    this.calendar = builder.calendar();
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

  /**
   * The validity of this alert, expressed as a set of time periods.
   */
  public AlertCalendar calendar() {
    return calendar;
  }

  /**
   * Checks if this alert is active at any point during the given {@code period}.
   * <p>
   * The alert does not need to be active for the entire period: it is enough that one of its
   * validity time periods overlaps the given period. In other words, this returns {@code true} as
   * long as there is at least one instant that is contained in both the given period and the
   * alert's validity.
   *
   * @param period the period to check for overlap with the alert's validity
   * @return true if the alert is active during any part of the given period
   */
  public boolean isActiveDuring(TimePeriod period) {
    return calendar.isActiveDuring(period);
  }

  /**
   * Checks if this alert is active at the given point in time.
   *
   * @param instant the point in time to check
   * @return true if the alert is active at the given time
   */
  public boolean isActiveAt(Instant instant) {
    return calendar.isActiveAt(instant);
  }

  /**
   * Finds the first validity start from all timePeriods for this alert.
   *
   * @return First start for this Alert, <code>null</code> if any period has an open start
   */
  @Nullable
  public Instant getEffectiveStartDate() {
    return calendar.effectiveStart().orElse(null);
  }

  /**
   * Finds the last validity end from all timePeriods for this alert. Returns <code>null</code>
   * if the validity is open-ended
   *
   * @return Last end for this Alert, <code>null</code> if open-ended
   */
  @Nullable
  public Instant getEffectiveEndDate() {
    return calendar.effectiveEnd().orElse(null);
  }

  @Override
  public boolean sameAs(TransitAlert other) {
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
      Objects.equals(calendar, other.calendar)
    );
  }

  @Override
  public TransitBuilder<TransitAlert, TransitAlertBuilder> copy() {
    return new TransitAlertBuilder(this);
  }
}
