package fi.metatavu.airquality;

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.locationtech.jts.geom.Envelope;

import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.units.DateUnit;

/**
 * Class for representing single air quality data file
 *
 * @author Antti Leppä <antti.leppa@metatavu.fi>
 * @author Heikki Kurhinen <heikki.kurhinen@metatavu.fi>
 *
 */
public class AirQualityDataFile {

  private OffsetDateTime originDate;
  private String error;
  private Array timeArray;
  private Array latitudeArray;
  private Array longitudeArray;
  private NetcdfPollution netcdfPollution;

  /**
   * Constructor for the class.
   * 
   * @param file NetCDF file containing air quality data
   */
  public AirQualityDataFile(File file) {
    error = null;

    try {
      NetcdfFile netcdfFile = readNetcdfFile(file);
      Variable time = netcdfFile.findVariable("time");
      Variable latitude = netcdfFile.findVariable("lat");
      Variable longitude = netcdfFile.findVariable("lon");

      Variable carbonMonoxide = netcdfFile.findVariable("daymax_cnc_CO");
      Variable nitrogenMonoxide = netcdfFile.findVariable("daymax_cnc_NO");
      Variable nitrogenDioxide = netcdfFile.findVariable("daymax_cnc_NO2");
      Variable ozone = netcdfFile.findVariable("daymax_cnc_O3");
      Variable sulfurDioxide = netcdfFile.findVariable("daymax_cnc_SO2");
      Variable particles10 = netcdfFile.findVariable("daymax_cnc_PM10");
      Variable particles2_5 = netcdfFile.findVariable("daymax_cnc_PM2_5");

      if (time == null) {
        error = String.format("Missing time variable from %s file", file.getAbsolutePath());
        return;
      }

      if (latitude == null) {
        error = String.format("Missing latitude variable from %s file", file.getAbsolutePath());
        return;
      }

      if (longitude == null) {
        error = String.format("Missing longitude variable from %s file", file.getAbsolutePath());
        return;
      }

      if (carbonMonoxide == null) {
        error = String.format("Missing carbon monoxide variable from %s file", file.getAbsolutePath());
        return;
      }

      if (nitrogenMonoxide == null) {
        error = String.format("Missing nitrogen monoxide variable from %s file", file.getAbsolutePath());
        return;
      }

      if (nitrogenDioxide == null) {
        error = String.format("Missing nitrogen dioxide variable from %s file", file.getAbsolutePath());
        return;
      }

      if (ozone == null) {
        error = String.format("Missing ozone variable from %s file", file.getAbsolutePath());
        return;
      }

      if (sulfurDioxide == null) {
        error = String.format("Missing sulfur dioxide variable from %s file", file.getAbsolutePath());
        return;
      }

      if (particles10 == null) {
        error = String.format("Missing PM10 variable from %s file", file.getAbsolutePath());
        return;
      }

      if (particles2_5 == null) {
        error = String.format("Missing PM2_5 variable from %s file", file.getAbsolutePath());
        return;
      }

      DateUnit dateUnit = new DateUnit(time.getUnitsString());
      Date dateOrigin = dateUnit.getDateOrigin();
      
      if (dateOrigin == null) {
        error = String.format("Missing origin date from %s file", file.getAbsolutePath());
        return;
      }
      
      originDate = OffsetDateTime.ofInstant(dateOrigin.toInstant(), ZoneId.systemDefault());
      timeArray = time.read();
      latitudeArray = latitude.read();
      longitudeArray = longitude.read();
      netcdfPollution = new NetcdfPollution(carbonMonoxide.read(), nitrogenMonoxide.read(), nitrogenDioxide.read(), ozone.read(), sulfurDioxide.read(), particles2_5.read(), particles10.read());
          
    } catch (Exception e) {
      error = e.getMessage();
    }
  }
  
  /**
   * Returns air quality data bounding box.
   * 
   * @return air quality data bounding box
   */
  public Envelope getBoundingBox() {
    double latitude1 = latitudeArray.getDouble(0);
    double longitude1 = longitudeArray.getDouble(0);
    double latitude2 = latitudeArray.getDouble((int) latitudeArray.getSize() - 1);
    double longitude2 = longitudeArray.getDouble((int) longitudeArray.getSize() - 1);
    return new Envelope(longitude1, longitude2, latitude1, latitude2);
  }
  
  /**
   * Returns whether the file was valid or not
   * 
   * @return whether the file was valid or not
   */
  public boolean isValid() {
    return error == null;
  }

  /**
   * Returns error if one is present in the file, null otherwise
   * 
   * @return error if one is present in the file.
   */
  public String getError() {
    return error;
  }

  /**
   * Returns time array
   * 
   * @return time array
   */
  public Array getTimeArray() {
    return timeArray;
  }

  /**
   * Returns latitude array
   * 
   * @return latitude array
   */
  public Array getLatitudeArray() {
    return latitudeArray;
  }

  /**
   * Returns longitude array
   * 
   * @return longitude array
   */
  public Array getLongitudeArray() {
    return longitudeArray;
  }
  
  /**
   * Returns origin date
   * 
   * @return origin date
   */
  public OffsetDateTime getOriginDate() {
    return originDate;
  }

  /**
   * Returns pollution
   *
   * @return pollution
   */
  public NetcdfPollution getPollution() { return netcdfPollution; }

  /**
   * Reads the NetCDF file
   * 
   * @param file file
   * @return Read file
   * @throws IOException throws Exception if file could not be read correctly
   */
  private NetcdfFile readNetcdfFile(File file) throws IOException {
    return NetcdfFile.open(file.getAbsolutePath());
  }

}
