package fi.metatavu.airquality;

import fi.metatavu.airquality.configuration_parsing.SingleConfig;
import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.ArrayFloat;

import java.awt.geom.Point2D;
import java.util.*;

/**
 * Class that updates the graph edges according to the data and configuration file provided
 */
public class GenericEdgeUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(GenericEdgeUpdater.class);

    private final GenericDataFile dataFile;
    private final Collection<StreetEdge> streetEdges;
    private final SingleConfig singleConfig;

    private final Map<String, ArrayFloat.D4> fileGenericData; //this data will update the street edges

    private int edgesUpdated;
    private final long dataStartTime = 0;

    public GenericEdgeUpdater (GenericDataFile dataFile, Collection<StreetEdge> streetEdges, SingleConfig config){
        super();
        this.dataFile = dataFile;
        this.streetEdges = streetEdges;
        this.singleConfig = config;

        this.edgesUpdated = 0;
        fileGenericData = dataFile.getGenericData();
        LOG.info(String.format("Street edges update starting from time stamp "));

    }

    /**
     * Updates generic data to street edges
     */
    public void updateEdges() {

        System.out.println("updating edges with air quality data");
        streetEdges.forEach(this::updateEdge);
    }
    /*
    update the edge according to the generic variable descriptions
     */
    private void updateEdge(StreetEdge streetEdge) {
        Vertex fromVertex = streetEdge.getFromVertex();
        Vertex toVertex = streetEdge.getToVertex();
        Coordinate fromCoordinate = fromVertex.getCoordinate();
        Coordinate toCoordinate = toVertex.getCoordinate();

        //set the edge values
        HashMap<String, float[]> edgeGenericDataValues = new HashMap<>();
        for (Map.Entry<String, ArrayFloat.D4> oneExtraDataTypeVals : fileGenericData.entrySet()) {
            float[] averageDataValue = getAveragePollution(fromCoordinate.x, fromCoordinate.y,
                    toCoordinate.x, toCoordinate.y, oneExtraDataTypeVals.getKey());
            edgeGenericDataValues.put(oneExtraDataTypeVals.getKey(), averageDataValue);
        }


        streetEdge.setAdditionalData(edgeGenericDataValues);

        edgesUpdated++;

    }
    /**
     * Returns average air quality sample data near given line.
     *
     * Each cell of returned array represent an average of air quality index in time
     *
     * @param fromLongitude from longitude
     * @param fromLatitude from latitude
     * @param toLongitude to longitude
     * @param toLatitude to latitude
     * @param pollutant pollutant
     *
     * @return array of pollutant values samples in time
     */
    private float[] getAveragePollution(double fromLongitude, double fromLatitude, double toLongitude, double toLatitude, String pollutant) {
        EdgeAirQuality edgeAirQuality = new EdgeAirQuality();

        getClosestSamples(fromLongitude, fromLatitude, toLongitude, toLatitude, pollutant).forEach(sample -> {
            for (int time = 0; time < sample.length; time++) {
                edgeAirQuality.addPollutantValueSample(time, sample[time]);
            }
        });

        return edgeAirQuality.getPollutantValues((int) dataFile.getTimeArray().getSize());
    }

    /**
     * Returns closest air quality samples for given line
     *
     * Each list cell represent an average of air quality index in time
     *
     * @param fromLongitude from longitude
     * @param fromLatitude from latitude
     * @param toLongitude to longitude
     * @param toLatitude to latitude
     * @param pollutant pollutant
     *
     * @return closest air quality samples for given line
     */
    private List<float[]> getClosestSamples(double fromLongitude, double fromLatitude, double toLongitude, double toLatitude, String pollutant) {
        List<float[]> result = new ArrayList<>();
        double azimuth = getAzimuth(fromLongitude, fromLatitude, toLongitude, toLatitude);
        double distance = getDistance(fromLongitude, fromLatitude, toLongitude, toLatitude);
        double spacing = 12d;

        for (int i = 0; i < distance / spacing; i++) {
            Point2D samplePoint = moveTo(fromLongitude, fromLatitude, azimuth, i * spacing);
            result.add(getClosestPollutionValue(samplePoint, pollutant));
        }

        return result;
    }


    /**
     * Returns closest a pollution value for given point.
     *
     * Returned array represent an air quality index in time
     *
     *
     * @param samplePoint point
     * @param pollutant pollutant
     *
     * @return closest pollution for given point
     */
    private float[] getClosestPollutionValue(Point2D samplePoint, String pollutant) {
        double lon = samplePoint.getX();
        double lat = samplePoint.getY();

        int lonIndex = getClosestIndex(dataFile.getLongitudeArray(), lon);
        int latIndex = getClosestIndex(dataFile.getLatitudeArray(), lat);

        int timeSize = (int) dataFile.getTimeArray().getSize();
        float[] result = new float[timeSize];
        int height = 0;

        for (int timeIndex = 0; timeIndex < timeSize; timeIndex++) {
            result[timeIndex] = fileGenericData.get(pollutant).get(timeIndex, height, latIndex, lonIndex);

        }

        return result;
    }

    /**
     * Returns closest index for a value from given array
     *
     * @param array array
     * @param value value
     * @return closest index
     */
    private int getClosestIndex(Array array, double value) {
        double distance = Double.MAX_VALUE;

        for (int i = 0; i < array.getSize(); i++) {
            double current = array.getDouble(i);
            double currentDistance = Math.abs(current - value);
            if (currentDistance < distance) {
                distance = currentDistance;
            } else {
                return i - 1;
            }
        }

        return (int) (array.getSize() - 1);
    }

    /**
     * Returns azimuth for given coordinates
     *
     * @param fromLongitude from longitude
     * @param fromLatitude from latitude
     * @param toLongitude to longitude
     * @param toLatitude to latitude
     * @return azimuth
     */
    private double getAzimuth(double fromLongitude, double fromLatitude, double toLongitude, double toLatitude) {
        GeodeticCalculator geodeticCalculator = new GeodeticCalculator();

        geodeticCalculator.setStartingGeographicPoint(fromLongitude, fromLatitude);
        geodeticCalculator.setDestinationGeographicPoint(toLongitude, toLatitude);

        return geodeticCalculator.getAzimuth();
    }

    /**
     * Returns distance between given coordinates
     *
     * @param fromLongitude from longitude
     * @param fromLatitude from latitude
     * @param toLongitude to longitude
     * @param toLatitude to latitude
     * @return distance between given coordinates
     */
    private double getDistance(double fromLongitude, double fromLatitude, double toLongitude, double toLatitude) {
        GeodeticCalculator geodeticCalculator = new GeodeticCalculator();

        geodeticCalculator.setStartingGeographicPoint(fromLongitude, fromLatitude);
        geodeticCalculator.setDestinationGeographicPoint(toLongitude, toLatitude);

        return geodeticCalculator.getOrthodromicDistance();
    }

    /**
     * Moves point a given amount from given coordinate towards given azimuth
     *
     * @param longitude longitude
     * @param latitude latitude
     * @param azimuth azimuth
     * @param amount amount
     * @return moved point
     */
    private Point2D moveTo(double longitude, double latitude, double azimuth, double amount) {
        GeodeticCalculator geodeticCalculator = new GeodeticCalculator();
        geodeticCalculator.setStartingGeographicPoint(longitude, latitude);
        geodeticCalculator.setDirection(azimuth, amount);
        return geodeticCalculator.getDestinationGeographicPoint();
    }

}
