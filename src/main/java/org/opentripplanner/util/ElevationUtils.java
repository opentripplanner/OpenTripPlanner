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

    /**
     * Computes the difference between the ellipsoid and geoid at a specified lat/lon using Geotools EarthGravitationalModel
     *
     * @param lat
     * @param lon
     * @return difference in meters
     * @throws FactoryException
     * @throws TransformException
     */

    public static double computeEllipsoidToGeoidDifference(double lat, double lon) throws FactoryException, TransformException {
        // Set up a MathTransform based on the EarthGravitationalModel
        EarthGravitationalModel.Provider provider = new EarthGravitationalModel.Provider();
        DefaultMathTransformFactory factory = new DefaultMathTransformFactory();
        MathTransform mt = factory.createParameterizedTransform(provider.getParameters().createValue());

        // Compute the offset
        DirectPosition3D dest = new DirectPosition3D();
        mt.transform(new DirectPosition3D(lon, lat, 0), dest);
        return dest.z;
    }
}
