package fi.metatavu.airquality;

import fi.metatavu.airquality.configuration_parsing.IndexVariable;
import fi.metatavu.airquality.configuration_parsing.SingleConfig;
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
    Map<String, ArrayFloat.D4> genericData = new HashMap<>();

    public GenericDataFile(File file, SingleConfig singleConfig) {
        error = null;

        //read the netcdf according to the settings in the provided configuration
        try {
            if (!file.exists()){
                error = String.format("Missing data file from %s file", file.getAbsolutePath());
            }
            NetcdfFile netcdfFile = readNetcdfFile(file);

            Variable time = netcdfFile.findVariable(singleConfig.getTimeVariable());
            Variable latitude = netcdfFile.findVariable(singleConfig.getLatitudeVariable());
            Variable longitude = netcdfFile.findVariable(singleConfig.getLongitudeVariable());

            //creating map of variables
            HashMap<IndexVariable, Variable> genVariables = new HashMap<>();
            for (IndexVariable indexVariable : singleConfig.getIndexVariables()){
                genVariables.put(indexVariable, netcdfFile.findVariable(indexVariable.getVariable()));
                //todo check if any vars from the conf are missing from the file?

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
            //read generic data into arrays
            for (Map.Entry<IndexVariable, Variable> genVariable : genVariables.entrySet()){
                genericData.put(genVariable.getKey().getName(), (ArrayFloat.D4) genVariable.getValue().read());
            }
        }
        catch (Exception e) {
            error = e.getMessage();
        }
    }


    public Map<String, ArrayFloat.D4> getGenericData() {
        return genericData;
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
