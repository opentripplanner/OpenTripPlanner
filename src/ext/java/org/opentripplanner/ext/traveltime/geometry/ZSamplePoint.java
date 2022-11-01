package org.opentripplanner.ext.traveltime.geometry;

public interface ZSamplePoint<TZ> {
  /**
   * @return The X index of this sample point.
   */
  int getX();

  /**
   * @return The Y index of this sample point.
   */
  int getY();

  /**
   * @return The Z value associated with this sample point.
   */
  TZ getZ();

  void setZ(TZ z);

  /**
   * @return The neighboring sample point located at (x,y-1)
   */
  ZSamplePoint<TZ> up();

  /**
   * @return The neighboring sample point located at (x,y+1)
   */
  ZSamplePoint<TZ> down();

  /**
   * @return The neighboring sample point located at (x+1,y)
   */
  ZSamplePoint<TZ> right();

  /**
   * @return The neighboring sample point located at (x-1,y)
   */
  ZSamplePoint<TZ> left();
}
