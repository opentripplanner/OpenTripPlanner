package org.opentripplanner.gbannotation;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class StreetCarSpeedZero extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 6872784791854835184L;

    public static final String FMT = "Way %s has car speed zero";
    
    final long way;
    
    @Override
    public String getMessage() {
        return String.format(FMT, way);
    }
}
