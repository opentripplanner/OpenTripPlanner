package org.opentripplanner.util;

import org.geotools.geometry.DirectPosition3D;
import org.geotools.referencing.operation.DefaultMathTransformFactory;
import org.geotools.referencing.operation.transform.EarthGravitationalModel;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * Created by demory on 2/16/17.
 */

public class ElevationUtils {

    // Set up a MathTransform based on the EarthGravitationalModel
    private static MathTransform mt;
    static {
        try {
            mt = new DefaultMathTransformFactory().createParameterizedTransform(
                new EarthGravitationalModel.Provider().getParameters().createValue()
            );
        } catch (FactoryException e) {
            e.printStackTrace();
        }
    }

    /**
     * Computes the difference between the ellipsoid and geoid at a specified lat/lon using Geotools
     * EarthGravitationalModel. For unknown reasons, this method can produce incorrect results if called at the same
     * time from multiple threads, so the method has been made synchronized.
     *
     * @param lat
     * @param lon
     * @return difference in meters
     * @throws FactoryException
     * @throws TransformException
     */
    public static synchronized double computeEllipsoidToGeoidDifference(double lat, double lon) throws TransformException {
        // Compute the offset
        DirectPosition3D dest = new DirectPosition3D();
        mt.transform(new DirectPosition3D(lon, lat, 0), dest);
        return dest.z;
    }
}
