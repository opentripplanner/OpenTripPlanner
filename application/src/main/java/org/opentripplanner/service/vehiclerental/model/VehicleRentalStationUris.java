package org.opentripplanner.service.vehiclerental.model;

import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Contains rental URIs for Android, iOS, and web in the android, ios, and web fields. See the
 * <a href="https://github.com/NABSA/gbfs/blob/v2.2/gbfs.md#station_informationjson">GBFS
 * station_information.json specification</a>
 * for more details.
 * <p>
 */
public final class VehicleRentalStationUris {

  public static final VehicleRentalStationUris DEFAULT = new VehicleRentalStationUris();

  /**
   * A URI that can be passed to an Android app with an {@code android.intent.action.VIEW} Android
   * intent to support Android Deep Links.
   * <p>
   * May be {@code null} if a rental URI does not exist.
   */
  @Nullable
  private final String android;

  /**
   * A URI that can be used on iOS to launch the rental app for this station.
   * <p>
   * May be {@code null} if a rental URI does not exist.
   */
  @Nullable
  private final String ios;

  /**
   * At URL that can be used by a web browser to show more information about renting a vehicle at
   * this station.
   * <p>
   * May be {@code null} if a rental URL does not exist.
   */
  @Nullable
  private final String web;

  private VehicleRentalStationUris() {
    this.android = null;
    this.ios = null;
    this.web = null;
  }

  private VehicleRentalStationUris(Builder builder) {
    this.android = builder.android;
    this.ios = builder.ios;
    this.web = builder.web;
  }

  public VehicleRentalStationUris(String android, String ios, String web) {
    this.android = android;
    this.ios = ios;
    this.web = web;
  }

  public static Builder of() {
    return DEFAULT.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  /**
   * A URI that can be passed to an Android app with an {@code android.intent.action.VIEW} Android
   * intent to support Android Deep Links.
   */
  @Nullable
  public String android() {
    return android;
  }

  /**
   * A URI that can be used on iOS to launch the rental app for this station.
   */
  @Nullable
  public String ios() {
    return ios;
  }

  /**
   * A URL that can be used by a web browser to show more information about renting a vehicle at
   * this station.
   */
  @Nullable
  public String web() {
    return web;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    VehicleRentalStationUris that = (VehicleRentalStationUris) o;
    return (
      Objects.equals(android, that.android) &&
      Objects.equals(ios, that.ios) &&
      Objects.equals(web, that.web)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(android, ios, web);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(VehicleRentalStationUris.class)
      .addStr("android", android, DEFAULT.android)
      .addStr("ios", ios, DEFAULT.ios)
      .addStr("web", web, DEFAULT.web)
      .toString();
  }

  public static class Builder {

    private final VehicleRentalStationUris original;
    private String android;
    private String ios;
    private String web;

    private Builder(VehicleRentalStationUris original) {
      this.original = original;
      this.android = original.android;
      this.ios = original.ios;
      this.web = original.web;
    }

    public String android() {
      return android;
    }

    public Builder withAndroid(@Nullable String android) {
      this.android = android;
      return this;
    }

    public String ios() {
      return ios;
    }

    public Builder withIos(@Nullable String ios) {
      this.ios = ios;
      return this;
    }

    public String web() {
      return web;
    }

    public Builder withWeb(@Nullable String web) {
      this.web = web;
      return this;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    public VehicleRentalStationUris build() {
      var value = new VehicleRentalStationUris(this);
      return original.equals(value) ? original : value;
    }
  }
}
