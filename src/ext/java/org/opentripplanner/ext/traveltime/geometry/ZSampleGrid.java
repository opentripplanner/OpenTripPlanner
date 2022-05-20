package org.opentripplanner.ext.traveltime.geometry;

import org.locationtech.jts.geom.Coordinate;

/**
 * A generic indexed grid of TZ samples. TZ could be anything but is usually a vector of parameters.
 *
 * We assume some sort of equirectangular project between the index coordinates (x,y) and the
 * geographic coordinates (lat, lon). The projection factor (cos phi, standard parallel) is given as
 * a cell size in lat,lon degrees (dLat,dLon)). The conversion is given by the following formulae:
 *
 * <code>
 * lon = lon0 + x.dLon;
 * lat = lat0 + y.dLat;
 * </code> (lat0,lon0) is the center, (dLat,dLon) is the cell size.
 *
 * @author laurent
 */
public interface ZSampleGrid<TZ> extends Iterable<ZSamplePoint<TZ>> {
  /**
   * @return The sample point located at (x,y). Create a new one if not existing.
   */
  ZSamplePoint<TZ> getOrCreate(int x, int y);

  /**
   * @param point The sample point
   * @return The (lat,lon) coordinates of this sample point.
   */
  Coordinate getCoordinates(ZSamplePoint<TZ> point);

  /**
   * @param C The geographical coordinate
   * @return The (x,y) index of the lower-left index of the cell enclosing the point.
   */
  int[] getLowerLeftIndex(Coordinate C);

  /**
   * @return The base coordinate center (lat0,lon0)
   */
  Coordinate getCenter();

  /**
   * @return The cell size (dLat,dLon)
   */
  public Coordinate getCellSize();

  public int getXMin();

  public int getXMax();

  public int getYMin();

  public int getYMax();

  int size();

  /**
   * TODO The mapping between a ZSampleGrid and a DelaunayTriangulation should not be part of an
   * interface but extracted to a converter. This assume that the conversion process does not rely
   * on the inner working of the ZSampleGrid implementation, which should be the case.
   *
   * @return This ZSampleGrid converted as a DelaunayTriangulation.
   */
  DelaunayTriangulation<TZ> delaunayTriangulate();
}
