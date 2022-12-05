package org.opentripplanner.api.model;

import java.io.Serializable;
import org.opentripplanner.framework.tostring.ToStringBuilder;

/**
 * This class calculates borders of envelopes that can be also on 180th meridian The same way as it
 * was previously calculated in GraphMetadata constructor
 */
public class ApiWorldEnvelope implements Serializable {

  private final double lowerLeftLatitude;
  private final double lowerLeftLongitude;
  private final double upperRightLatitude;
  private final double upperRightLongitude;

  public ApiWorldEnvelope(
    double lowerLeftLatitude,
    double lowerLeftLongitude,
    double upperRightLatitude,
    double upperRightLongitude
  ) {
    this.lowerLeftLatitude = lowerLeftLatitude;
    this.lowerLeftLongitude = lowerLeftLongitude;
    this.upperRightLatitude = upperRightLatitude;
    this.upperRightLongitude = upperRightLongitude;
  }

  public double getLowerLeftLatitude() {
    return lowerLeftLatitude;
  }

  public double getLowerLeftLongitude() {
    return lowerLeftLongitude;
  }

  public double getUpperRightLatitude() {
    return upperRightLatitude;
  }

  public double getUpperRightLongitude() {
    return upperRightLongitude;
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(ApiWorldEnvelope.class)
      .addObj("lowerLeft", "[" + lowerLeftLatitude + ", " + lowerLeftLongitude + "]")
      .addObj("upperRight", "[" + upperRightLatitude + ", " + upperRightLongitude + "]")
      .toString();
  }
}
