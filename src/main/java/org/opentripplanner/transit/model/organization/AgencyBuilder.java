package org.opentripplanner.transit.model.organization;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.basic.FeedScopedId;
import org.opentripplanner.transit.model.basic.TransitEntityBuilder;

public class AgencyBuilder extends TransitEntityBuilder<Agency, AgencyBuilder> {

  private String name;
  private String timezone;
  private String url;
  private String lang;
  private String phone;
  private String fareUrl;
  private String brandingUrl;

  AgencyBuilder(FeedScopedId id) {
    super(id);
  }

  AgencyBuilder(@Nullable Agency agency) {
    super(agency);
  }

  public String getName() {
    return name;
  }

  public AgencyBuilder withName(String name) {
    this.name = name;
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

  @Override
  protected void updateLocal(@Nonnull Agency original) {
    this.name = original.getName();
    this.timezone = original.getTimezone();
    this.url = original.getUrl();
    this.lang = original.getLang();
    this.phone = original.getPhone();
    this.fareUrl = original.getFareUrl();
    this.brandingUrl = original.getBrandingUrl();
  }
}
