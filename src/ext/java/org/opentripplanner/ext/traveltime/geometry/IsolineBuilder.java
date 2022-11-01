package org.opentripplanner.ext.traveltime.geometry;

import org.locationtech.jts.geom.Geometry;

/**
 * Generic interface for a class that compute an isoline out of a TZ 2D "field".
 *
 * @author laurent
 */
public interface IsolineBuilder<TZ> {
  Geometry computeIsoline(TZ z0);
}
