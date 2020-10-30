package fi.metatavu.airquality;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.geotools.referencing.GeodeticCalculator;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.locationtech.jts.geom.Coordinate;

import ucar.ma2.Array;

/**
 * Class that updates air quality data from single air quality data file into all street edges.
 *
 */
public class AirQualityEdgeUpdater {

  private static final Logger LOG = LoggerFactory.getLogger(AirQualityEdgeUpdater.class);
  private static final int REPORT_EVERY_N_EDGE = 10000;

  private final AirQualityDataFile airQualityDataFile;
  private final Collection<StreetEdge> streetEdges;
  private int edgesUpdated;
  private final long dataStartTime;
  private final NetcdfPollution netcdfPollution;

  /**
   * Constructor for air quality edge updater
   *
   * @param airQualityDataFile data file
   * @param streetEdges street edges
   */
  public AirQualityEdgeUpdater(AirQualityDataFile airQualityDataFile, Collection<StreetEdge> streetEdges) {
    super();
    this.airQualityDataFile = airQualityDataFile;
    this.streetEdges = streetEdges;
    this.edgesUpdated = 0;
    double startTimeHours = this.airQualityDataFile.getTimeArray().getDouble(0);
    this.dataStartTime = this.airQualityDataFile.getOriginDate().toInstant().plusSeconds((long) (startTimeHours * 3600)).toEpochMilli();
    netcdfPollution = airQualityDataFile.getPollution();
    LOG.info(String.format("Street edges update starting from time stamp %d", this.dataStartTime));
  }

  /**
   * Updates air quality data to street edges
   */
  public void updateEdges() {
    streetEdges.forEach(this::updateEdge);
  }

  /**
   * Updates air quality data into single edge
   *
   * @param streetEdge edge
   */
  private void updateEdge(StreetEdge streetEdge) {
    Vertex fromVertex = streetEdge.getFromVertex();
    Vertex toVertex = streetEdge.getToVertex();
    Coordinate fromCoordinate = fromVertex.getCoordinate();
    Coordinate toCoordinate = toVertex.getCoordinate();

    float[] carbonMonoxide = getAveragePollution(fromCoordinate.x, fromCoordinate.y, toCoordinate.x, toCoordinate.y, Pollutant.CARBON_MONOXIDE);
    float[] nitrogenMonoxide = getAveragePollution(fromCoordinate.x, fromCoordinate.y, toCoordinate.x, toCoordinate.y, Pollutant.NITROGEN_MONOXIDE);
    float[] nitrogenDioxide = getAveragePollution(fromCoordinate.x, fromCoordinate.y, toCoordinate.x, toCoordinate.y, Pollutant.NITROGEN_DIOXIDE);
    float[] ozone = getAveragePollution(fromCoordinate.x, fromCoordinate.y, toCoordinate.x, toCoordinate.y, Pollutant.OZONE);
    float[] sulfurDioxide = getAveragePollution(fromCoordinate.x, fromCoordinate.y, toCoordinate.x, toCoordinate.y, Pollutant.SULFUR_DIOXIDE);
    float[] particles2_5 = getAveragePollution(fromCoordinate.x, fromCoordinate.y, toCoordinate.x, toCoordinate.y, Pollutant.PARTICLES_PM2_5);
    float[] particles10 = getAveragePollution(fromCoordinate.x, fromCoordinate.y, toCoordinate.x, toCoordinate.y, Pollutant.PARTICLES_PM10);

    streetEdge.setCarbonMonoxide(carbonMonoxide);
    streetEdge.setNitrogenMonoxide(nitrogenMonoxide);
    streetEdge.setNitrogenDioxide(nitrogenDioxide);
    streetEdge.setOzone(ozone);
    streetEdge.setSulfurDioxide(sulfurDioxide);
    streetEdge.setParticles2_5(particles2_5);
    streetEdge.setParticles10(particles10);
    streetEdge.setAirQualityDataStartTime(dataStartTime);

    edgesUpdated++;

    if (LOG.isInfoEnabled() && (edgesUpdated % REPORT_EVERY_N_EDGE) == 0) {
      LOG.info(String.format("%d / %d street edges updated", edgesUpdated, streetEdges.size()));
    }
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
  private float[] getAveragePollution(double fromLongitude, double fromLatitude, double toLongitude, double toLatitude, Pollutant pollutant) {
    EdgeAirQuality edgeAirQuality = new EdgeAirQuality();

    getClosestSamples(fromLongitude, fromLatitude, toLongitude, toLatitude, pollutant).forEach(sample -> {
      for (int time = 0; time < sample.length; time++) {
        edgeAirQuality.addPollutantValueSample(time, sample[time]);
      }
    });

    return edgeAirQuality.getPollutantValues((int) airQualityDataFile.getTimeArray().getSize());
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
  private List<float[]> getClosestSamples(double fromLongitude, double fromLatitude, double toLongitude, double toLatitude, Pollutant pollutant) {
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
   * @param samplePoint point
   * @param pollutant pollutant
   *
   * @return closest pollution for given point
   */
  private float[] getClosestPollutionValue(Point2D samplePoint, Pollutant pollutant) {
    double lon = samplePoint.getX();
    double lat = samplePoint.getY();

    int lonIndex = getClosestIndex(airQualityDataFile.getLongitudeArray(), lon);
    int latIndex = getClosestIndex(airQualityDataFile.getLatitudeArray(), lat);

    int timeSize = (int) airQualityDataFile.getTimeArray().getSize();
    float[] result = new float[timeSize];
    int height = 0;

    for (int timeIndex = 0; timeIndex < timeSize; timeIndex++) {
      if (pollutant == Pollutant.CARBON_MONOXIDE) {
        result[timeIndex] = netcdfPollution.getCarbonMonoxide().get(timeIndex, height, latIndex, lonIndex);
      }

      if (pollutant == Pollutant.NITROGEN_MONOXIDE) {
        result[timeIndex] = netcdfPollution.getNitrogenMonoxide().get(timeIndex, height, latIndex, lonIndex);
      }

      if (pollutant == Pollutant.NITROGEN_DIOXIDE) {
        result[timeIndex] = netcdfPollution.getNitrogenDioxide().get(timeIndex, height, latIndex, lonIndex);
      }

      if (pollutant == Pollutant.OZONE) {
        result[timeIndex] = netcdfPollution.getOzone().get(timeIndex, height, latIndex, lonIndex);
      }

      if (pollutant == Pollutant.SULFUR_DIOXIDE) {
        result[timeIndex] = netcdfPollution.getSulfurDioxide().get(timeIndex, height, latIndex, lonIndex);
      }

      if (pollutant == Pollutant.PARTICLES_PM2_5) {
        result[timeIndex] = netcdfPollution.getParticles2_5().get(timeIndex, height, latIndex, lonIndex);
      }

      if (pollutant == Pollutant.PARTICLES_PM10) {
        result[timeIndex] = netcdfPollution.getParticles10().get(timeIndex, height, latIndex, lonIndex);
      }
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

