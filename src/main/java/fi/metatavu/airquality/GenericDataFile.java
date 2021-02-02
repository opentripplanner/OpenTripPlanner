package fi.metatavu.airquality;

import fi.metatavu.airquality.configuration_parsing.IndexVariable;
import fi.metatavu.airquality.configuration_parsing.GenericFileConfiguration;
import ucar.ma2.Array;
import ucar.ma2.ArrayFloat;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.units.DateUnit;

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Generic data file which is read according to graphs/*settings.json settings
 */
public class GenericDataFile{

    private OffsetDateTime originDate;
    private String error;
    private ucar.ma2.Array timeArray;
    private ucar.ma2.Array latitudeArray;
    private Array longitudeArray;
    /*
    storage of the parsed data, where key = name of the peoperty, e.g. o2 and value is the
    array of its data
     */
    Map<String, ArrayFloat.D4> netcdfData = new HashMap<>();

    public GenericDataFile(File file, GenericFileConfiguration genericFileConfiguration) {
        error = null;

        //read the netcdf according to the settings in the provided configuration
        try {
            if (!file.exists()){
                error = String.format("Missing data file from %s file", file.getAbsolutePath());
                return;
            }
            NetcdfFile netcdfFile = readNetcdfFile(file);

            //read universal variables which always are present
            Variable time = netcdfFile.findVariable(genericFileConfiguration.getTimeVariable());

            if (time == null) {
                error = String.format("Missing time variable from %s file", file.getAbsolutePath());
                return;
            }

            Variable latitude = netcdfFile.findVariable(genericFileConfiguration.getLatitudeVariable());
            Variable longitude = netcdfFile.findVariable(genericFileConfiguration.getLongitudeVariable());

            //creating map of variables
            HashMap<IndexVariable, Variable> genVariables = new HashMap<>();
            for (IndexVariable indexVariable : genericFileConfiguration.getIndexVariables()){
                genVariables.put(indexVariable, netcdfFile.findVariable(indexVariable.getVariable()));
                //todo check if any vars from the conf are missing from the file?

            }


            DateUnit dateUnit = new DateUnit(time.getUnitsString());
            Date dateOrigin = dateUnit.getDateOrigin();


            if (dateOrigin == null) {
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

            originDate = OffsetDateTime.ofInstant(dateOrigin.toInstant(), ZoneId.systemDefault());
            timeArray = time.read();
            latitudeArray = latitude.read();
            longitudeArray = longitude.read();
            //read generic data into arrays
            for (Map.Entry<IndexVariable, Variable> genVariable : genVariables.entrySet()){
                netcdfData.put(genVariable.getKey().getName(), (ArrayFloat.D4) genVariable.getValue().read());
            }
        }
        catch (Exception e) {
            error = e.getMessage();
        }
    }


    public Map<String, ArrayFloat.D4> getNetcdfData() {
        return netcdfData;
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
