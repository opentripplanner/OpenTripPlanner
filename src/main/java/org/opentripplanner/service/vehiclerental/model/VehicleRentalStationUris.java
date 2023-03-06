package org.opentripplanner.service.vehiclerental.model;

import javax.annotation.Nullable;

/**
 * Contains rental URIs for Android, iOS, and web in the android, ios, and web fields. See the
 * <a href="https://github.com/NABSA/gbfs/blob/v2.2/gbfs.md#station_informationjson">GBFS
 * station_information.json specification</a>
 * for more details.
 */
public class VehicleRentalStationUris {

  /**
   * A URI that can be passed to an Android app with an {@code android.intent.action.VIEW} Android
   * intent to support Android Deep Links.
   * <p>
   * May be {@code null} if a rental URI does not exist.
   */
  @Nullable
  public final String android;

  /**
   * A URI that can be used on iOS to launch the rental app for this station.
   * <p>
   * May be {@code null} if a rental URI does not exist.
   */
  @Nullable
  public final String ios;

  /**
   * At URL that can be used by a web browser to show more information about renting a vehicle at
   * this station.
   * <p>
   * May be {@code null} if a rental URL does not exist.
   */
  @Nullable
  public final String web;

  public VehicleRentalStationUris(String android, String ios, String web) {
    this.android = android;
    this.ios = ios;
    this.web = web;
  }
}
