package org.opentripplanner.updater.vehicle_rental.datasources.params;

import java.util.Map;
import javax.annotation.Nonnull;
import org.opentripplanner.updater.DataSourceType;

public class VehicleRentalDataSourceParameters {

  private final DataSourceType sourceType;
  private final String url;
  private final Map<String, String> httpHeaders;

  public VehicleRentalDataSourceParameters(
    DataSourceType sourceType,
    String url,
    @Nonnull Map<String, String> httpHeaders
  ) {
    this.sourceType = sourceType;
    this.url = url;
    this.httpHeaders = httpHeaders;
  }

  @Nonnull
  public String getUrl() {
    return url;
  }

  @Nonnull
  public DataSourceType getSourceType() {
    return sourceType;
  }

  @Nonnull
  public Map<String, String> getHttpHeaders() {
    return httpHeaders;
  }
}
