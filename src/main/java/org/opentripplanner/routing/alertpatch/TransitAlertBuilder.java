package org.opentripplanner.routing.alertpatch;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.framework.AbstractEntityBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class TransitAlertBuilder extends AbstractEntityBuilder<TransitAlert, TransitAlertBuilder> {

  private I18NString headerText;
  private I18NString descriptionText;
  private I18NString detailText;
  private I18NString adviceText;
  private I18NString url;
  private final List<AlertUrl> siriUrls = new ArrayList<>();
  private String type;
  private AlertSeverity severity;
  private AlertCause cause;
  private AlertEffect effect;
  //null means unknown
  private Integer priority;
  private ZonedDateTime creationTime;
  private ZonedDateTime updatedTime;
  private String siriCodespace;
  private Integer version;
  private final Set<EntitySelector> entities = new HashSet<>();
  private final List<TimePeriod> timePeriods = new ArrayList<>();

  TransitAlertBuilder(FeedScopedId id) {
    super(id);
  }

  TransitAlertBuilder(TransitAlert original) {
    super(original);
    this.headerText = original.headerText().orElse(null);
    this.descriptionText = original.descriptionText().orElse(null);
    this.detailText = original.detailText();
    this.adviceText = original.adviceText();
    this.url = original.url().orElse(null);
    this.siriUrls.addAll(original.siriUrls());
    this.type = original.type();
    this.severity = original.severity();
    this.cause = original.cause();
    this.effect = original.effect();
    this.priority = original.priority();
    this.creationTime = original.creationTime();
    this.updatedTime = original.updatedTime();
    this.siriCodespace = original.siriCodespace();
    this.entities.addAll(original.entities());
    this.timePeriods.addAll(original.timePeriods());
  }

  public I18NString headerText() {
    return headerText;
  }

  public TransitAlertBuilder withHeaderText(I18NString headerText) {
    this.headerText = headerText;
    return this;
  }

  public I18NString descriptionText() {
    return descriptionText;
  }

  public TransitAlertBuilder withDescriptionText(I18NString descriptionText) {
    this.descriptionText = descriptionText;
    return this;
  }

  public I18NString detailText() {
    return detailText;
  }

  public TransitAlertBuilder withDetailText(I18NString detailText) {
    this.detailText = detailText;
    return this;
  }

  public I18NString adviceText() {
    return adviceText;
  }

  public TransitAlertBuilder withAdviceText(I18NString adviceText) {
    this.adviceText = adviceText;
    return this;
  }

  public I18NString url() {
    return url;
  }

  public TransitAlertBuilder withUrl(I18NString gtfsUrl) {
    this.url = gtfsUrl;
    return this;
  }

  public List<AlertUrl> siriUrls() {
    return siriUrls;
  }

  public TransitAlertBuilder addSiriUrl(AlertUrl alertUrl) {
    siriUrls.add(alertUrl);
    return this;
  }

  public TransitAlertBuilder addSiriUrls(Collection<AlertUrl> alertUrls) {
    siriUrls.addAll(alertUrls);
    return this;
  }

  public String type() {
    return type;
  }

  public TransitAlertBuilder withType(String type) {
    this.type = type;
    return this;
  }

  public AlertSeverity severity() {
    return severity;
  }

  public TransitAlertBuilder withSeverity(AlertSeverity severity) {
    this.severity = severity;
    return this;
  }

  public AlertCause cause() {
    return cause;
  }

  public TransitAlertBuilder withCause(AlertCause cause) {
    this.cause = cause;
    return this;
  }

  public AlertEffect effect() {
    return effect;
  }

  public TransitAlertBuilder withEffect(AlertEffect effect) {
    this.effect = effect;
    return this;
  }

  public Integer priority() {
    return priority;
  }

  public TransitAlertBuilder withPriority(Integer priority) {
    this.priority = priority;
    return this;
  }

  public ZonedDateTime creationTime() {
    return creationTime;
  }

  public TransitAlertBuilder withCreationTime(ZonedDateTime creationTime) {
    this.creationTime = creationTime;
    return this;
  }

  public Integer version() {
    return version;
  }

  public TransitAlertBuilder withVersion(Integer version) {
    this.version = version;
    return this;
  }

  public ZonedDateTime updatedTime() {
    return updatedTime;
  }

  public TransitAlertBuilder withUpdatedTime(ZonedDateTime updatedTime) {
    this.updatedTime = updatedTime;
    return this;
  }

  public String siriCodespace() {
    return siriCodespace;
  }

  public TransitAlertBuilder withSiriCodespace(String siriCodespace) {
    this.siriCodespace = siriCodespace;
    return this;
  }

  public Set<EntitySelector> entities() {
    return entities;
  }

  public boolean hasEntities() {
    return !entities.isEmpty();
  }

  public TransitAlertBuilder addEntity(EntitySelector entitySelector) {
    entities.add(entitySelector);
    return this;
  }

  public TransitAlertBuilder addEntites(List<EntitySelector> entitySelectors) {
    entities.addAll(entitySelectors);
    return this;
  }

  public Collection<TimePeriod> timePeriods() {
    return timePeriods;
  }

  public TransitAlertBuilder addTimePeriod(TimePeriod timePeriod) {
    timePeriods.add(timePeriod);
    return this;
  }

  public TransitAlertBuilder addTimePeriods(Collection<TimePeriod> periods) {
    timePeriods.addAll(periods);
    return this;
  }

  @Override
  protected TransitAlert buildFromValues() {
    return new TransitAlert(this);
  }
}
