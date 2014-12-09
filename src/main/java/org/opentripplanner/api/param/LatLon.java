package org.opentripplanner.api.param;

public class LatLon extends QueryParameter {

    public double lat;
    public double lon;

    public LatLon (String value) {
        super(value);
    }

    @Override
    protected void parse(String value) throws Throwable {
        if(value == null || value.isEmpty())
            throw new Exception("Null or empty latitude/longitude pair");

        String[] fields = value.split(",");
        lat = Double.parseDouble(fields[0]);
        lon = Double.parseDouble(fields[1]);
        checkRangeInclusive(lat,  -90,  90);
        checkRangeInclusive(lon, -180, 180);
    }

    @Override
    public String toString() {
        return String.format("(%f,%f)", lat, lon);
    }

}