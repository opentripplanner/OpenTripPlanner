package org.opentripplanner.ext.restapi.model;

/**
 * Represent type of vertex, used in Place aka from, to in API for easier client side localization
 *
 * @author mabu
 */
public enum ApiVertexType {
  NORMAL,
  BIKESHARE,
  BIKEPARK,
  TRANSIT,
}
