package org.opentripplanner.ext.dataoverlay;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.opentripplanner.ext.dataoverlay.configuration.DataOverlayConfig;
import org.opentripplanner.ext.dataoverlay.configuration.IndexVariable;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.units.DateUnit;

/**
 * Generic data file which is read according to data-overlay-config.json settings
 *
 * @author Katja Danilova
 */
public class GenericDataFile {

  private final String dataSource;
  private OffsetDateTime originDate;
  private String error = null;
  private ucar.ma2.Array timeArray;
  private ucar.ma2.Array latitudeArray;
  private Array longitudeArray;
  private Map<String, Array> netcdfDataForVariable;

  /**
   * Reads and parses the .nc file according to configuration into map of variable names and arrays
   * of their values from the .nc file
   *
   * @param file              input .nc data grid file
   * @param dataOverlayConfig settings which describe the file variables selection
   */
  public GenericDataFile(File file, DataOverlayConfig dataOverlayConfig) {
    this.dataSource = file.getPath();

    try {
      if (!file.exists()) {
        error = String.format("Missing data file from %s file", file.getAbsolutePath());
        return;
      }

      NetcdfFile netcdfFile = readNetcdfFile(file);

      Variable time = netcdfFile.findVariable(dataOverlayConfig.getTimeVariable());

      if (time == null) {
        error = String.format("Missing time variable from %s file", file.getAbsolutePath());
        return;
      }

      Variable latitude = netcdfFile.findVariable(dataOverlayConfig.getLatitudeVariable());
      Variable longitude = netcdfFile.findVariable(dataOverlayConfig.getLongitudeVariable());

      HashMap<IndexVariable, Variable> genVariables = new HashMap<>();
      for (IndexVariable indexVariable : dataOverlayConfig.getIndexVariables()) {
        genVariables.put(indexVariable, netcdfFile.findVariable(indexVariable.getVariable()));
        //todo check if any vars from the configuration are missing from the file?
      }

      DateUnit dateUnit = new DateUnit(time.getUnitsString());
      Optional<Instant> dateOrigin = Optional.ofNullable(dateUnit.getDateOrigin()).map(
        Date::toInstant
      );

      if (dateOrigin.isEmpty()) {
        error = String.format("Missing origin date from %s file", file.getAbsolutePath());
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

      originDate = OffsetDateTime.ofInstant(dateOrigin.get(), ZoneId.systemDefault());
      timeArray = time.read();
      latitudeArray = latitude.read();
      longitudeArray = longitude.read();

      netcdfDataForVariable = new HashMap<>(genVariables.size());
      for (Map.Entry<IndexVariable, Variable> genVariable : genVariables.entrySet()) {
        netcdfDataForVariable.put(genVariable.getKey().getName(), genVariable.getValue().read());
      }
    } catch (Exception e) {
      error = e.getMessage();
    }
  }

  public Map<String, Array> getNetcdfDataForVariable() {
    return netcdfDataForVariable;
  }

  public boolean isValid() {
    return error == null;
  }

  public String getError() {
    return error;
  }

  public Array getTimeArray() {
    return timeArray;
  }

  public Array getLatitudeArray() {
    return latitudeArray;
  }

  public Array getLongitudeArray() {
    return longitudeArray;
  }

  public OffsetDateTime getOriginDate() {
    return originDate;
  }

  String getDataSource() {
    return dataSource;
  }

  /**
   * Reads the NetCDF file
   *
   * @param file file
   * @return Read file
   * @throws IOException throws Exception if file could not be read correctly
   */
  private NetcdfFile readNetcdfFile(File file) throws IOException {
    return NetcdfFiles.open(file.getAbsolutePath());
  }
}
