package org.opentripplanner.api.mapping;

import org.opentripplanner.api.model.ApiVertexType;
import org.opentripplanner.model.plan.VertexType;

public class VertexTypeMapper {

    public static ApiVertexType mapVertexType(VertexType domain) {
        if(domain == null) { return null; }
        switch (domain) {
            case NORMAL: return ApiVertexType.NORMAL;
            case VEHICLEPARKING: return ApiVertexType.BIKEPARK;
            case VEHICLERENTAL: return ApiVertexType.BIKESHARE;
            case TRANSIT: return ApiVertexType.TRANSIT;
            default:
                throw new IllegalArgumentException(domain.toString());
        }
    }
}
