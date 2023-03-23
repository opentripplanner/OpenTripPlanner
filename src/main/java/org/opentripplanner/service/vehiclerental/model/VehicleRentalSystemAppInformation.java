package org.opentripplanner.service.vehiclerental.model;

/**
 * Based on the field rental_apps in {@ https://github.com/NABSA/gbfs/blob/master/gbfs.md#system_informationjson
 */
public class VehicleRentalSystemAppInformation {

  public final String storeUri;
  public final String discoveryUri;

  public VehicleRentalSystemAppInformation(String storeUri, String discoveryUri) {
    this.storeUri = storeUri;
    this.discoveryUri = discoveryUri;
  }
}
