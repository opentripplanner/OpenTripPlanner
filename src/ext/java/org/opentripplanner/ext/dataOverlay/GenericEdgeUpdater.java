package org.opentripplanner.ext.dataOverlay;

import org.opentripplanner.ext.dataOverlay.configuration.DavaOverlayConfig;
import org.opentripplanner.ext.dataOverlay.configuration.TimeUnit;
import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;

import java.awt.geom.Point2D;
import java.time.Instant;
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
    private final DavaOverlayConfig davaOverlayConfig;

    private final Map<String, Array> genericVariablesData;
    private int edgesUpdated;
    private final long dataStartTime;

    /**
     * Calculates the generic data start time and sets the earlier parsed map of generic data file
     *
     * @param dataFile map of generic grid data from .nc file
     * @param fileConfiguration configuration for data file
     * @param streetEdges collection of all street edges to be updated
     */
    public GenericEdgeUpdater(GenericDataFile dataFile, DavaOverlayConfig fileConfiguration, Collection<StreetEdge> streetEdges){
        super();
        this.dataFile = dataFile;
        this.streetEdges = streetEdges;
        this.davaOverlayConfig = fileConfiguration;
        this.edgesUpdated = 0;

        this.dataStartTime = calculateDataStartTime(fileConfiguration);
        genericVariablesData = dataFile.getNetcdfDataForVariable();
        LOG.info(String.format("Street edges update from %s starting from time stamp %d", fileConfiguration.getFileName(), this.dataStartTime));
    }

    /**
     * Returns ms from epoch for the first data point of the file, by default data format is assumed to be hours
     *
     * @param configuration configuration
     * @return epoch milliseconds
     */
    private long calculateDataStartTime(DavaOverlayConfig configuration) {
        TimeUnit timeFormat = configuration.getTimeFormat();
        Array timeArray = dataFile.getTimeArray();
        Class dataType = timeArray.getDataType().getPrimitiveClassType();
        Instant originInstant = this.dataFile.getOriginDate().toInstant();

        if ((timeFormat == null || timeFormat == TimeUnit.SECONDS)
            && dataType.equals(Integer.TYPE)) {
            return originInstant.plusSeconds(timeArray.getInt(0)).toEpochMilli();
        } else if (timeFormat == TimeUnit.MS_EPOCH && dataType.equals(Long.TYPE)) {
            return timeArray.getLong(0);
        } else {
            long addSeconds = 0;
            if  (dataType.equals(Double.TYPE)) {
                addSeconds = (long) (timeArray.getDouble(0) * 3600);
            } else if (dataType.equals(Float.TYPE)) {
                addSeconds = (long) (timeArray.getFloat(0) * 3600);
            }

            return originInstant.plusSeconds(addSeconds).toEpochMilli();
        }
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

        EdgeDataFromGenericFile edgeGenData = new EdgeDataFromGenericFile(dataStartTime, edgeGenericDataValues, davaOverlayConfig.getTimeFormat());
        streetEdge.setExtraData(edgeGenData);

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
     * Returns the closest property value samples for given line
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
     * @param samplePoint point
     * @param propertyName propertyName
     * @return result the closest value for selected property for given point
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
            Index selectIndex = dataArray.getIndex();

            if (selectIndex.getRank() == 3) {
                selectIndex.set(timeIndex, latIndex, lonIndex);
            }
            else if (selectIndex.getRank() == 4) {
                selectIndex.set(timeIndex, height, latIndex, lonIndex);
            }
            else throw new IllegalArgumentException(String.format("Invalid data array shape for %s", propertyName));

            Class dataArrayType = dataArray.getDataType().getPrimitiveClassType();
            if (dataArrayType.equals(Integer.TYPE))
                result.add(timeIndex, ((ArrayInt) dataArray).get(selectIndex));
            else if (dataArrayType.equals(Double.TYPE))
                result.add(timeIndex, ((ArrayDouble) dataArray).get(selectIndex));
            else if (dataArrayType.equals(Float.TYPE))
                result.add(timeIndex, ((ArrayFloat) dataArray).get(selectIndex));
            else if (dataArrayType.equals(Long.TYPE))
                result.add(timeIndex, ((ArrayLong) dataArray).get(selectIndex));
            else
                throw new IllegalArgumentException(String.format("Unsupported format %s of %s variable", dataArrayType, propertyName));
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
