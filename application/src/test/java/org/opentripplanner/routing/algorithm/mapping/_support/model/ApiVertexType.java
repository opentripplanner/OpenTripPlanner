package org.opentripplanner.routing.algorithm.mapping._support.model;

/**
 * Represent type of vertex, used in Place aka from, to in API for easier client side localization
 *
 * @author mabu
 */
@Deprecated
public enum ApiVertexType {
  NORMAL,
  BIKESHARE,
  BIKEPARK,
  TRANSIT,
}
