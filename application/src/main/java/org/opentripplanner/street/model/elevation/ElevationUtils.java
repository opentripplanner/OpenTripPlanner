package org.opentripplanner.street.model.elevation;

import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.Position3D;
import org.geotools.referencing.operation.DefaultMathTransformFactory;
import org.geotools.referencing.operation.transform.EarthGravitationalModel;

/**
 * Created by demory on 2/16/17.
 */

public class ElevationUtils {

  // Set up a MathTransform based on the EarthGravitationalModel
  private static MathTransform mt;

  static {
    try {
      mt = new DefaultMathTransformFactory()
        .createParameterizedTransform(
          new EarthGravitationalModel.Provider().getParameters().createValue()
        );
    } catch (FactoryException e) {
      e.printStackTrace();
    }
  }

  /**
   * Computes the difference between the ellipsoid and geoid at a specified lat/lon using Geotools
   * EarthGravitationalModel. For unknown reasons, this method can produce incorrect results if
   * called at the same time from multiple threads, so the method has been made synchronized.
   *
   * @return difference in meters
   */
  public static synchronized double computeEllipsoidToGeoidDifference(double lat, double lon)
    throws TransformException {
    // Compute the offset
    Position3D dest = new Position3D();
    mt.transform(new Position3D(lon, lat, 0), dest);
    return dest.z;
  }
}
