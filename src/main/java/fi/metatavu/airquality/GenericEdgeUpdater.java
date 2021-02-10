package fi.metatavu.airquality;

import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;

import java.awt.geom.Point2D;
import java.util.*;

/**
 * Class that updates the graph edges according to the generic grid data and configuration file provided
 *
 * @author Simeon Platonov
 */
public class GenericEdgeUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(GenericEdgeUpdater.class);
    private static final int REPORT_EVERY_N_EDGE = 10000;

    private final GenericDataFile dataFile;
    private final Collection<StreetEdge> streetEdges;

    private final Map<String, Array> genericVariablesData;
    private int edgesUpdated;
    private final long dataStartTime;

    /**
     * Calculates the generic data start time and sets the earlier parsed map of generic data file
     *
     * @param dataFile map of generic grid data from .nc file
     * @param streetEdges collection of all street edges to be updated
     */
    public GenericEdgeUpdater(GenericDataFile dataFile, Collection<StreetEdge> streetEdges){
        super();
        this.dataFile = dataFile;
        this.streetEdges = streetEdges;

        this.edgesUpdated = 0;
        double startTimeHours = this.dataFile.getTimeArray().getDouble(0);
        this.dataStartTime = this.dataFile.getOriginDate().toInstant().plusSeconds((long) (startTimeHours * 3600)).toEpochMilli();

        genericVariablesData = dataFile.getNetcdfDataForVariable();
        LOG.info(String.format("Street edges update starting from time stamp %d", this.dataStartTime));
    }

    /**
     * Updates generic data to street edges
     */
    public void updateEdges() {
        streetEdges.forEach(this::updateEdge);
    }

    /**
     * Updates the edge according to the generic variable data
     *
     * @param streetEdge street edge being updated with extra data
     */
    private void updateEdge(StreetEdge streetEdge) {
        Vertex fromVertex = streetEdge.getFromVertex();
        Vertex toVertex = streetEdge.getToVertex();
        Coordinate fromCoordinate = fromVertex.getCoordinate();
        Coordinate toCoordinate = toVertex.getCoordinate();

        HashMap<String, float[]> edgeGenericDataValues = new HashMap<>();
        for (Map.Entry<String, Array> variableValues : genericVariablesData.entrySet()) {
            float[] averageDataValue = getAverageValue(fromCoordinate.x, fromCoordinate.y,
                    toCoordinate.x, toCoordinate.y, variableValues.getKey());
            edgeGenericDataValues.put(variableValues.getKey(), averageDataValue);
        }

        EdgeDataFromGenericFile edgeGenData = new EdgeDataFromGenericFile(dataStartTime, edgeGenericDataValues);
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
     * @return array of propertyName values samples in time
     */
    private float[] getAverageValue(double fromLongitude, double fromLatitude, double toLongitude, double toLatitude, String propertyName) {
        EdgeGenQuality<Number> edgeGenQuality =  new EdgeGenQuality<>();

        getClosestSamples(fromLongitude, fromLatitude, toLongitude, toLatitude, propertyName)
                .forEach(sample -> {
                    for (int time = 0; time < sample.size(); time++) {
                        edgeGenQuality.addPropertyValueSample(time, (Number) sample.get(time));
                    }
                });

        return edgeGenQuality.getPropertyValueAverage((int) dataFile.getTimeArray().getSize());
    }

    /**
     * Returns closest property value samples for given line
     *
     * Each list cell represent an average of grid data in time
     *
     * @param fromLongitude from longitude
     * @param fromLatitude from latitude
     * @param toLongitude to longitude
     * @param toLatitude to latitude
     * @param propertyName propertyName
     * @return closest grid data samples for given line
     */
    private <E> List<List<E>> getClosestSamples(double fromLongitude, double fromLatitude, double toLongitude, double toLatitude, String propertyName) {
        List<List<E>> result = new ArrayList<>();
        double azimuth = getAzimuth(fromLongitude, fromLatitude, toLongitude, toLatitude);
        double distance = getDistance(fromLongitude, fromLatitude, toLongitude, toLatitude);
        double spacing = 12d;

        for (int i = 0; i < distance / spacing; i++) {
            Point2D samplePoint = moveTo(fromLongitude, fromLatitude, azimuth, i * spacing);
            List<E> closestPropertyValue = getClosestPropertyValue(samplePoint, propertyName);
            result.add(closestPropertyValue);
        }

        return result;
    }

    /**
     * Returns closest a value of selected property for given point.
     *
     * Returned array represent value for selected property time
     *
     * originally it was float []
     * @param samplePoint point
     * @param propertyName propertyName
     * @return result closest value for selected property for given point
     */
    private <E> List<E> getClosestPropertyValue(Point2D samplePoint, String propertyName) {
        double lon = samplePoint.getX();
        double lat = samplePoint.getY();

        int lonIndex = getClosestIndex(dataFile.getLongitudeArray(), lon);
        int latIndex = getClosestIndex(dataFile.getLatitudeArray(), lat);

        int timeSize = (int) dataFile.getTimeArray().getSize();
        List result = new ArrayList<>();
        int height = 0;

        for (int timeIndex = 0; timeIndex < timeSize; timeIndex++) {
            Array dataArray = genericVariablesData.get(propertyName);

            if (dataArray instanceof ArrayInt.D4)
                result.add(timeIndex, ((ArrayInt.D4) dataArray).get(timeIndex, height, latIndex, lonIndex));
            else if (dataArray instanceof ArrayDouble.D4)
                result.add(timeIndex, ((ArrayDouble.D4) dataArray).get(timeIndex, height, latIndex, lonIndex));
            else if (dataArray instanceof ArrayFloat.D4)
                result.add(timeIndex, ((ArrayFloat.D4) dataArray).get(timeIndex, height, latIndex, lonIndex));
            else if (dataArray instanceof ArrayLong.D4)
                result.add(timeIndex, ((ArrayLong.D4) dataArray).get(timeIndex, height, latIndex, lonIndex));
            else
                throw new IllegalArgumentException(String.format("Unsupported format of %s variable", propertyName));
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
