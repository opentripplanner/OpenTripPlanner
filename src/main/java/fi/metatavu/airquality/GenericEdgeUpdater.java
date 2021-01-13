package fi.metatavu.airquality;

import fi.metatavu.airquality.configuration_parsing.GenericFileConfiguration;
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
    private static final int REPORT_EVERY_N_EDGE = 10000;

    private final GenericDataFile dataFile;
    private final Collection<StreetEdge> streetEdges;

    private final String configurationName;
    private final Map<String, ArrayFloat.D4> fileGenericData; //this data will update the street edges

    private int edgesUpdated;
    private final long dataStartTime;

    public GenericEdgeUpdater(GenericDataFile dataFile, Collection<StreetEdge> streetEdges,
                              String configurationName){
        super();
        this.dataFile = dataFile;
        this.streetEdges = streetEdges;
        this.configurationName = configurationName;

        this.edgesUpdated = 0;
        double startTimeHours = this.dataFile.getTimeArray().getDouble(0);
        this.dataStartTime = this.dataFile.getOriginDate().toInstant().plusSeconds((long) (startTimeHours * 3600)).toEpochMilli();

        fileGenericData = dataFile.getNetcdfData();
        LOG.info(String.format("Street edges update with "+configurationName+" starting from time stamp %d", this.dataStartTime));
    }

    /**
     * Updates generic data to street edges
     */
    public void updateEdges() {
        streetEdges.forEach(this::updateEdge);
    }

    /**
     *  Updates the edge according to the generic variable descriptions
     * @param streetEdge
     */
    private void updateEdge(StreetEdge streetEdge) {
        Vertex fromVertex = streetEdge.getFromVertex();
        Vertex toVertex = streetEdge.getToVertex();
        Coordinate fromCoordinate = fromVertex.getCoordinate();
        Coordinate toCoordinate = toVertex.getCoordinate();

        //calculate average generic values for each of the variables from configuration file
        HashMap<String, float[]> edgeGenericDataValues = new HashMap<>();
        for (Map.Entry<String, ArrayFloat.D4> variableValues : fileGenericData.entrySet()) {
            float[] averageDataValue = getAverageValue(fromCoordinate.x, fromCoordinate.y,
                    toCoordinate.x, toCoordinate.y, variableValues.getKey());
            edgeGenericDataValues.put(variableValues.getKey(), averageDataValue);
        }

        EdgeDataFromGenericFile edgeGenData = new EdgeDataFromGenericFile(configurationName,
                dataStartTime, edgeGenericDataValues);
        streetEdge.getExtraData().add(edgeGenData);

        edgesUpdated++;

        if (LOG.isInfoEnabled() && (edgesUpdated % REPORT_EVERY_N_EDGE) == 0) {
            LOG.info(String.format("%d / %d street edges updated", edgesUpdated, streetEdges.size()));
        }

    }
    /**
     * Returns average property sample data near given line.
     *
     * Each cell of returned array represent an average of quality of the selected property in time
     *
     * @param fromLongitude from longitude
     * @param fromLatitude from latitude
     * @param toLongitude to longitude
     * @param toLatitude to latitude
     * @param propertyName propertyName
     *
     * @return array of propertyName values samples in time
     */
    private float[] getAverageValue(double fromLongitude, double fromLatitude, double toLongitude, double toLatitude, String propertyName) {
        EdgeGenQuality edgeGenQuality = new EdgeGenQuality();

        getClosestSamples(fromLongitude, fromLatitude, toLongitude, toLatitude, propertyName).forEach(sample -> {
            for (int time = 0; time < sample.length; time++) {
                edgeGenQuality.addPropertyValueSample(time, sample[time]);
            }
        });

        return edgeGenQuality.getPeroptyValuesAverages((int) dataFile.getTimeArray().getSize());
    }

    /**
     * Returns closest property value samples for given line
     *
     * Each list cell represent an average of air quality index in time
     *
     * @param fromLongitude from longitude
     * @param fromLatitude from latitude
     * @param toLongitude to longitude
     * @param toLatitude to latitude
     * @param propertyName propertyName
     *
     * @return closest air quality samples for given line
     */
    private List<float[]> getClosestSamples(double fromLongitude, double fromLatitude, double toLongitude, double toLatitude, String propertyName) {
        List<float[]> result = new ArrayList<>();
        double azimuth = getAzimuth(fromLongitude, fromLatitude, toLongitude, toLatitude);
        double distance = getDistance(fromLongitude, fromLatitude, toLongitude, toLatitude);
        double spacing = 12d;

        for (int i = 0; i < distance / spacing; i++) {
            Point2D samplePoint = moveTo(fromLongitude, fromLatitude, azimuth, i * spacing);
            result.add(getClosestPropertyValue(samplePoint, propertyName));
        }

        return result;
    }


    /**
     * Returns closest a value of selected property for given point.
     *
     * Returned array represent value for selected property time
     *
     *
     * @param samplePoint point
     * @param propertyName propertyName
     *
     * @return closest value for selected property for given point
     */
    private float[] getClosestPropertyValue(Point2D samplePoint, String propertyName) {
        double lon = samplePoint.getX();
        double lat = samplePoint.getY();

        int lonIndex = getClosestIndex(dataFile.getLongitudeArray(), lon);
        int latIndex = getClosestIndex(dataFile.getLatitudeArray(), lat);

        int timeSize = (int) dataFile.getTimeArray().getSize();
        float[] result = new float[timeSize];
        int height = 0;

        for (int timeIndex = 0; timeIndex < timeSize; timeIndex++) {
            result[timeIndex] = fileGenericData.get(propertyName).get(timeIndex, height, latIndex, lonIndex);

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
