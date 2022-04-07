package org.opentripplanner.ext.dataoverlay.api;

/**
 * This is a white-list of supported parameters. The purpose is to standardize the parameter names
 * and allow API users/client applications to implement support for this across deployments.
 * <p>
 * There are two types of each parameter, a penalty and a threshold. So, you will not find {@code
 * ozone} on the API or in config. Instead there will be the two types, {@code ozone_} and {@code
 * ozone_}.
 * <p>
 * If the parameter you need is not listed, you can ask for  it  to be added. Make a PR or issue, or
 * contact the community.
 */
public enum ParameterName {
  AIR_QUALITY_INDEX,
  PARTICULATE_MATTER_2_5,
  PARTICULATE_MATTER_10,
  CARBON_MONOXIDE,
  NITROGEN_MONOXIDE,
  NITROGEN_DIOXIDE,
  SULFUR_MONOXIDE,
  SULFUR_DIOXIDE,
  OZONE,
  LEAD,
  TEMPERATURE,
  HUMIDITY,
  WIND_SPEED,
}
