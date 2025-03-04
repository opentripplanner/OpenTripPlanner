package org.opentripplanner.ext.dataoverlay.configuration;

import java.io.Serializable;
import java.util.List;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * POJO class describing expected data-overlay-config.json structure
 *
 * @author Katja Danilova
 */
public class DataOverlayConfig implements Serializable {

  private final String fileName;
  private final String latitudeVariable;
  private final String longitudeVariable;
  private final String timeVariable;
  private final TimeUnit timeFormat;
  private final List<IndexVariable> indexVariables;
  private final DataOverlayParameterBindings requestParameters;

  public DataOverlayConfig(
    String fileName,
    String latitudeVariable,
    String longitudeVariable,
    String timeVariable,
    TimeUnit timeFormat,
    List<IndexVariable> indexVariables,
    List<ParameterBinding> requestParameters
  ) {
    this.fileName = fileName;
    this.latitudeVariable = latitudeVariable;
    this.longitudeVariable = longitudeVariable;
    this.timeVariable = timeVariable;
    this.timeFormat = timeFormat;
    this.indexVariables = indexVariables;
    this.requestParameters = new DataOverlayParameterBindings(requestParameters);
  }

  /**
   * Gets file path to the .nc data file
   *
   * @return path to the .nc data file
   */
  public String getFileName() {
    return fileName;
  }

  /**
   * Gets latitude variable.
   *
   * @return the latitude variable
   */
  public String getLatitudeVariable() {
    return latitudeVariable;
  }

  /**
   * Gets longitude variable.
   *
   * @return the longitude variable
   */
  public String getLongitudeVariable() {
    return longitudeVariable;
  }

  /**
   * Gets time variable.
   *
   * @return the time variable
   */
  public String getTimeVariable() {
    return timeVariable;
  }

  /**
   * Gets time format
   *
   * @return the time format enum
   */
  public TimeUnit getTimeFormat() {
    return timeFormat;
  }

  /**
   * Get index variables index variable [ ].
   *
   * @return the index variable [ ]
   */
  public List<IndexVariable> getIndexVariables() {
    return indexVariables;
  }

  /**
   * Get request parameters request parameters [ ].
   *
   * @return the request parameters [ ]
   */
  public DataOverlayParameterBindings getParameterBindings() {
    return requestParameters;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(DataOverlayConfig.class)
      .addStr("fileName", fileName)
      .addStr("latitudeVariable", latitudeVariable)
      .addStr("longitudeVariable", longitudeVariable)
      .addStr("timeVariable", timeVariable)
      .addEnum("timeFormat", timeFormat)
      .addCol("indexVariables", indexVariables)
      .addObj("requestParameters", requestParameters)
      .toString();
  }
}
