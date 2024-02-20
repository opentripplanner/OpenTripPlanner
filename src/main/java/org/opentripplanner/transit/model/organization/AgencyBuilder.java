package org.opentripplanner.transit.model.organization;

import javax.annotation.Nonnull;
import org.opentripplanner.transit.model.framework.AbstractEntityBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class AgencyBuilder extends AbstractEntityBuilder<Agency, AgencyBuilder> {

  private String name;
  private String shortName;
  private String timezone;
  private String url;
  private String lang;
  private String phone;
  private String fareUrl;
  private String brandingUrl;

  AgencyBuilder(FeedScopedId id) {
    super(id);
  }

  AgencyBuilder(@Nonnull Agency original) {
    super(original);
    this.name = original.getName();
    this.shortName = original.getShortName();
    this.timezone = original.getTimezone().getId();
    this.url = original.getUrl();
    this.lang = original.getLang();
    this.phone = original.getPhone();
    this.fareUrl = original.getFareUrl();
    this.brandingUrl = original.getBrandingUrl();
  }

  public String getName() {
    return name;
  }
  public String getShortName() {
    return shortName;
  }

  public AgencyBuilder withName(String name) {
    this.name = name;
    return this;
  }
  public AgencyBuilder withShortName(String shortName) {
    this.shortName = shortName;
    return this;
  }

  public String getTimezone() {
    return timezone;
  }

  public AgencyBuilder withTimezone(String timezone) {
    this.timezone = timezone;
    return this;
  }

  public String getUrl() {
    return url;
  }

  public AgencyBuilder withUrl(String url) {
    this.url = url;
    return this;
  }

  public String getLang() {
    return lang;
  }

  public AgencyBuilder withLang(String lang) {
    this.lang = lang;
    return this;
  }

  public String getPhone() {
    return phone;
  }

  public AgencyBuilder withPhone(String phone) {
    this.phone = phone;
    return this;
  }

  public String getFareUrl() {
    return fareUrl;
  }

  public AgencyBuilder withFareUrl(String fareUrl) {
    this.fareUrl = fareUrl;
    return this;
  }

  public String getBrandingUrl() {
    return brandingUrl;
  }

  public AgencyBuilder withBrandingUrl(String brandingUrl) {
    this.brandingUrl = brandingUrl;
    return this;
  }

  @Override
  protected Agency buildFromValues() {
    return new Agency(this);
  }
}
