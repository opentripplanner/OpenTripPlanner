package org.opentripplanner.service.vehiclerental.model;

import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Based on https://github.com/NABSA/gbfs/blob/master/gbfs.md#system_informationjson
 * <p>
 */
public final class VehicleRentalSystem {

  public static final VehicleRentalSystem DEFAULT = new VehicleRentalSystem();

  @Nullable
  private final String systemId;

  @Nullable
  private final String language;

  @Nullable
  private final String name;

  @Nullable
  private final String shortName;

  @Nullable
  private final String operator;

  @Nullable
  private final String url;

  @Nullable
  private final String purchaseUrl;

  @Nullable
  private final String startDate;

  @Nullable
  private final String phoneNumber;

  @Nullable
  private final String email;

  @Nullable
  private final String feedContactEmail;

  @Nullable
  private final String licenseUrl;

  // this is intentionally a string, not a zone id as the validator doesn't check the correctness
  // https://github.com/MobilityData/gbfs-validator/issues/76
  private final String timezone;
  private final VehicleRentalSystemAppInformation androidApp;
  private final VehicleRentalSystemAppInformation iosApp;

  private VehicleRentalSystem() {
    this.systemId = null;
    this.language = null;
    this.name = null;
    this.shortName = null;
    this.operator = null;
    this.url = null;
    this.purchaseUrl = null;
    this.startDate = null;
    this.phoneNumber = null;
    this.email = null;
    this.feedContactEmail = null;
    this.licenseUrl = null;
    this.timezone = null;
    this.androidApp = null;
    this.iosApp = null;
  }

  private VehicleRentalSystem(Builder builder) {
    this.systemId = builder.systemId;
    this.language = builder.language;
    this.name = builder.name;
    this.shortName = builder.shortName;
    this.operator = builder.operator;
    this.url = builder.url;
    this.purchaseUrl = builder.purchaseUrl;
    this.startDate = builder.startDate;
    this.phoneNumber = builder.phoneNumber;
    this.email = builder.email;
    this.feedContactEmail = builder.feedContactEmail;
    this.timezone = builder.timezone;
    this.licenseUrl = builder.licenseUrl;
    this.androidApp = builder.androidApp;
    this.iosApp = builder.iosApp;
  }

  public VehicleRentalSystem(
    String systemId,
    String language,
    String name,
    String shortName,
    String operator,
    String url,
    String purchaseUrl,
    String startDate,
    String phoneNumber,
    String email,
    String feedContactEmail,
    String timezone,
    String licenseUrl,
    VehicleRentalSystemAppInformation androidApp,
    VehicleRentalSystemAppInformation iosApp
  ) {
    this.systemId = systemId;
    this.language = language;
    this.name = name;
    this.shortName = shortName;
    this.operator = operator;
    this.url = url;
    this.purchaseUrl = purchaseUrl;
    this.startDate = startDate;
    this.phoneNumber = phoneNumber;
    this.email = email;
    this.feedContactEmail = feedContactEmail;
    this.timezone = timezone;
    this.licenseUrl = licenseUrl;
    this.androidApp = androidApp;
    this.iosApp = iosApp;
  }

  public static Builder of() {
    return DEFAULT.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  @Nullable
  public String systemId() {
    return systemId;
  }

  @Nullable
  public String language() {
    return language;
  }

  @Nullable
  public String name() {
    return name;
  }

  @Nullable
  public String shortName() {
    return shortName;
  }

  @Nullable
  public String operator() {
    return operator;
  }

  @Nullable
  public String url() {
    return url;
  }

  @Nullable
  public String purchaseUrl() {
    return purchaseUrl;
  }

  @Nullable
  public String startDate() {
    return startDate;
  }

  @Nullable
  public String phoneNumber() {
    return phoneNumber;
  }

  @Nullable
  public String email() {
    return email;
  }

  @Nullable
  public String feedContactEmail() {
    return feedContactEmail;
  }

  @Nullable
  public String licenseUrl() {
    return licenseUrl;
  }

  /**
   * This is intentionally a string, not a zone id as the validator doesn't check the correctness.
   * @see <a href="https://github.com/MobilityData/gbfs-validator/issues/76">GBFS Validator Issue 76</a>
   */
  @Nullable
  public String timezone() {
    return timezone;
  }

  @Nullable
  public VehicleRentalSystemAppInformation androidApp() {
    return androidApp;
  }

  @Nullable
  public VehicleRentalSystemAppInformation iosApp() {
    return iosApp;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    VehicleRentalSystem that = (VehicleRentalSystem) o;
    return (
      Objects.equals(systemId, that.systemId) &&
      Objects.equals(language, that.language) &&
      Objects.equals(name, that.name) &&
      Objects.equals(shortName, that.shortName) &&
      Objects.equals(operator, that.operator) &&
      Objects.equals(url, that.url) &&
      Objects.equals(purchaseUrl, that.purchaseUrl) &&
      Objects.equals(startDate, that.startDate) &&
      Objects.equals(phoneNumber, that.phoneNumber) &&
      Objects.equals(email, that.email) &&
      Objects.equals(feedContactEmail, that.feedContactEmail) &&
      Objects.equals(licenseUrl, that.licenseUrl) &&
      Objects.equals(timezone, that.timezone) &&
      Objects.equals(androidApp, that.androidApp) &&
      Objects.equals(iosApp, that.iosApp)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      systemId,
      language,
      name,
      shortName,
      operator,
      url,
      purchaseUrl,
      startDate,
      phoneNumber,
      email,
      feedContactEmail,
      licenseUrl,
      timezone,
      androidApp,
      iosApp
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(VehicleRentalSystem.class)
      .addStr("systemId", systemId, DEFAULT.systemId)
      .addStr("language", language, DEFAULT.language)
      .addStr("name", name, DEFAULT.name)
      .addStr("shortName", shortName, DEFAULT.shortName)
      .addStr("operator", operator, DEFAULT.operator)
      .addStr("url", url, DEFAULT.url)
      .addStr("purchaseUrl", purchaseUrl, DEFAULT.purchaseUrl)
      .addStr("startDate", startDate, DEFAULT.startDate)
      .addStr("phoneNumber", phoneNumber, DEFAULT.phoneNumber)
      .addStr("email", email, DEFAULT.email)
      .addStr("feedContactEmail", feedContactEmail, DEFAULT.feedContactEmail)
      .addStr("licenseUrl", licenseUrl, DEFAULT.licenseUrl)
      .addStr("timezone", timezone, DEFAULT.timezone)
      .addObj("androidApp", androidApp, DEFAULT.androidApp)
      .addObj("iosApp", iosApp, DEFAULT.iosApp)
      .toString();
  }

  public static class Builder {

    private final VehicleRentalSystem original;
    private String systemId;
    private String language;
    private String name;
    private String shortName;
    private String operator;
    private String url;
    private String purchaseUrl;
    private String startDate;
    private String phoneNumber;
    private String email;
    private String feedContactEmail;
    private String licenseUrl;
    private String timezone;
    private VehicleRentalSystemAppInformation androidApp;
    private VehicleRentalSystemAppInformation iosApp;

    private Builder(VehicleRentalSystem original) {
      this.original = original;
      this.systemId = original.systemId;
      this.language = original.language;
      this.name = original.name;
      this.shortName = original.shortName;
      this.operator = original.operator;
      this.url = original.url;
      this.purchaseUrl = original.purchaseUrl;
      this.startDate = original.startDate;
      this.phoneNumber = original.phoneNumber;
      this.email = original.email;
      this.feedContactEmail = original.feedContactEmail;
      this.licenseUrl = original.licenseUrl;
      this.timezone = original.timezone;
      this.androidApp = original.androidApp;
      this.iosApp = original.iosApp;
    }

    public String systemId() {
      return systemId;
    }

    public Builder withSystemId(@Nullable String systemId) {
      this.systemId = systemId;
      return this;
    }

    public String language() {
      return language;
    }

    public Builder withLanguage(@Nullable String language) {
      this.language = language;
      return this;
    }

    public String name() {
      return name;
    }

    public Builder withName(@Nullable String name) {
      this.name = name;
      return this;
    }

    public String shortName() {
      return shortName;
    }

    public Builder withShortName(@Nullable String shortName) {
      this.shortName = shortName;
      return this;
    }

    public String operator() {
      return operator;
    }

    public Builder withOperator(@Nullable String operator) {
      this.operator = operator;
      return this;
    }

    public String url() {
      return url;
    }

    public Builder withUrl(@Nullable String url) {
      this.url = url;
      return this;
    }

    public String purchaseUrl() {
      return purchaseUrl;
    }

    public Builder withPurchaseUrl(@Nullable String purchaseUrl) {
      this.purchaseUrl = purchaseUrl;
      return this;
    }

    public String startDate() {
      return startDate;
    }

    public Builder withStartDate(@Nullable String startDate) {
      this.startDate = startDate;
      return this;
    }

    public String phoneNumber() {
      return phoneNumber;
    }

    public Builder withPhoneNumber(@Nullable String phoneNumber) {
      this.phoneNumber = phoneNumber;
      return this;
    }

    public String email() {
      return email;
    }

    public Builder withEmail(@Nullable String email) {
      this.email = email;
      return this;
    }

    public String feedContactEmail() {
      return feedContactEmail;
    }

    public Builder withFeedContactEmail(@Nullable String feedContactEmail) {
      this.feedContactEmail = feedContactEmail;
      return this;
    }

    public String licenseUrl() {
      return licenseUrl;
    }

    public Builder withLicenseUrl(@Nullable String licenseUrl) {
      this.licenseUrl = licenseUrl;
      return this;
    }

    public String timezone() {
      return timezone;
    }

    public Builder withTimezone(@Nullable String timezone) {
      this.timezone = timezone;
      return this;
    }

    public VehicleRentalSystemAppInformation androidApp() {
      return androidApp;
    }

    public Builder withAndroidApp(@Nullable VehicleRentalSystemAppInformation androidApp) {
      this.androidApp = androidApp;
      return this;
    }

    public VehicleRentalSystemAppInformation iosApp() {
      return iosApp;
    }

    public Builder withIosApp(@Nullable VehicleRentalSystemAppInformation iosApp) {
      this.iosApp = iosApp;
      return this;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    public VehicleRentalSystem build() {
      var value = new VehicleRentalSystem(this);
      return original.equals(value) ? original : value;
    }
  }
}
