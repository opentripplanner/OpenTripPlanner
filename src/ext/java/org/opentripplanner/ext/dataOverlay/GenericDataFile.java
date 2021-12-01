package org.opentripplanner.ext.dataOverlay;

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.ext.dataOverlay.configuration.DavaOverlayConfig;
import org.opentripplanner.ext.dataOverlay.configuration.IndexVariable;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.units.DateUnit;

/**
 * Generic data file which is read according to graphs/data-settings.json settings
 *
 * @author Katja Danilova
 */
public class GenericDataFile {

    private OffsetDateTime originDate;
    private String error;
    private ucar.ma2.Array timeArray;
    private ucar.ma2.Array latitudeArray;
    private Array longitudeArray;
    private Map<String, Array> netcdfDataForVariable;

    /**
     * Reads and parses the .nc file according to configuration into map of variable names and
     * arrays of their values from the .nc file
     *
     * @param file              input .nc data grid file
     * @param davaOverlayConfig settings which describe the file variables selection
     */
    public GenericDataFile(File file, DavaOverlayConfig davaOverlayConfig) {
        error = null;

        try {
            if (!file.exists()) {
                error = String.format("Missing data file from %s file", file.getAbsolutePath());
                return;
            }

            NetcdfFile netcdfFile = readNetcdfFile(file);

            Variable time = netcdfFile.findVariable(davaOverlayConfig.getTimeVariable());

            if (time == null) {
                error = String.format("Missing time variable from %s file", file.getAbsolutePath());
                return;
            }

            Variable latitude = netcdfFile.findVariable(davaOverlayConfig.getLatitudeVariable());
            Variable longitude = netcdfFile.findVariable(davaOverlayConfig.getLongitudeVariable());

            HashMap<IndexVariable, Variable> genVariables = new HashMap<>();
            for (IndexVariable indexVariable : davaOverlayConfig.getIndexVariables()) {
                genVariables.put(
                        indexVariable, netcdfFile.findVariable(indexVariable.getVariable()));
                //todo check if any vars from the configuration are missing from the file?
            }

            DateUnit dateUnit = new DateUnit(time.getUnitsString());
            Date dateOrigin = dateUnit.getDateOrigin();

            if (dateOrigin == null) {
                error = String.format("Missing origin date from %s file", file.getAbsolutePath());
                return;
            }

            if (latitude == null) {
                error = String.format("Missing latitude variable from %s file",
                        file.getAbsolutePath()
                );
                return;
            }

            if (longitude == null) {
                error = String.format("Missing longitude variable from %s file",
                        file.getAbsolutePath()
                );
                return;
            }

            originDate = OffsetDateTime.ofInstant(dateOrigin.toInstant(), ZoneId.systemDefault());
            timeArray = time.read();
            latitudeArray = latitude.read();
            longitudeArray = longitude.read();

            netcdfDataForVariable = new HashMap<>(genVariables.size());
            for (Map.Entry<IndexVariable, Variable> genVariable : genVariables.entrySet()) {
                netcdfDataForVariable.put(
                        genVariable.getKey().getName(), genVariable.getValue().read());
            }
        }
        catch (Exception e) {
            error = e.getMessage();
        }
    }

    /**
     * Gets the map of variable names and arrays of corresponding data from .nc file
     *
     * @return map of variables names and corresponding grid data arrays
     */
    public Map<String, Array> getNetcdfDataForVariable() {
        return netcdfDataForVariable;
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
