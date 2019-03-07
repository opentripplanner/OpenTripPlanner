package org.opentripplanner.model.json_serialization;

import org.locationtech.jts.geom.Geometry;

/**
 * Enunciate requires that we actually be able to construct objects, but 
 * we won't ever need to use this functionality, since the relevant APIs
 * don't support XML.  This class is a fake factory class for geometries 
 * to fool Enunciate. 
 * @author novalis
 *
 */
public class BogusGeometryFactory {
    public Geometry neverCreateGeometry() {
        return null;
    }
}
