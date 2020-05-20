package org.opentripplanner.standalone.config;

import org.opentripplanner.ext.siri.updater.SiriETHttpTripUpdateSource;
import org.opentripplanner.ext.siri.updater.SiriVMHttpTripUpdateSource;
import org.opentripplanner.updater.bike_park.KmlBikeParkDataSource;
import org.opentripplanner.updater.bike_rental.BicimadBikeRentalDataSource;
import org.opentripplanner.updater.bike_rental.CityBikesBikeRentalDataSource;
import org.opentripplanner.updater.bike_rental.GbfsBikeRentalDataSource;
import org.opentripplanner.updater.bike_rental.GenericJsonBikeRentalDataSource;
import org.opentripplanner.updater.bike_rental.GenericKmlBikeRentalDataSource;
import org.opentripplanner.updater.bike_rental.GenericXmlBikeRentalDataSource;

/**
 * This class is an object representation of the data source for a single real-time updater in
 * 'router-config.json' Each data source defines an inner interface with its required attributes.
 */
public class UpdaterDataSourceConfig implements
    BicimadBikeRentalDataSource.BicimadBikeRentalDataSourceConfig,
    CityBikesBikeRentalDataSource.CityBikesBikeRentalDataSourceConfig,
    GenericXmlBikeRentalDataSource.GenericXmlBikeRentalDataSourceConfig,
    GbfsBikeRentalDataSource.GbfsBikeRentalDataSourceConfig,
    GenericKmlBikeRentalDataSource.GenericKmlBikeRentalDataSourceConfig,
    KmlBikeParkDataSource.KmlBikeParkDataSourceConfig,
    SiriETHttpTripUpdateSource.SiriETHttpTripUpdateSourceConfig,
    SiriVMHttpTripUpdateSource.SiriVMHttpTripUpdateSourceConfig,
    GenericJsonBikeRentalDataSource.GenericJsonBikeRentalDataSourceConfig
{

  private final String name;
  private final String url;
  private final boolean routeAsCar;
  private final String namePrefix;
  private final String file;
  private final String feedId;
  private final boolean zip;
  private final String requestorRef;
  private final int timeoutSec;
  private final int previewIntervalMinutes;

  public UpdaterDataSourceConfig() {
    name = null;
    url = null;
    routeAsCar = false;
    namePrefix = null;
    file = null;
    feedId = null;
    zip = false;
    requestorRef = null;
    timeoutSec = 0;
    previewIntervalMinutes = 0;
  }

  public UpdaterDataSourceConfig(NodeAdapter c) {
    this.name = c.asText("sourceType", null);
    this.url = c.asText("url", null);
    this.routeAsCar = c.asBoolean("routeAsCar", false);
    this.namePrefix = c.asText("namePrefix", null);
    this.file = c.asText("file", null);
    this.feedId = c.asText("feedId", null);
    this.zip = c.asBoolean("zip", false);
    this.requestorRef = c.asText("requestorRef", null);
    this.timeoutSec = c.asInt("timeoutSec", 0);
    this.previewIntervalMinutes = c.asInt("previewIntervalMinutes", 0);
  }

  public String getName() {
    return name;
  }

  public String getUrl() {
    return url;
  }

  public boolean routeAsCar() {
    return routeAsCar;
  }

  public String getNamePrefix() {
    return namePrefix;
  }

  public String getFile() {
    return file;
  }

  public String getFeedId() {
    return feedId;
  }

  public boolean zip() {
    return zip;
  }

  public String getRequestorRef() {
    return requestorRef;
  }

  public int getTimeoutSec() {
    return timeoutSec;
  }

  public int getPreviewIntervalMinutes() {
    return previewIntervalMinutes;
  }
}
