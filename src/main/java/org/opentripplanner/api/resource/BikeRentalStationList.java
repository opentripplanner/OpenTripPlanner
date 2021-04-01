package org.opentripplanner.api.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.updater.RentalUpdaterError;
import org.opentripplanner.updater.vehicle_rental.GBFSMappings.SystemInformation;

public class BikeRentalStationList {
    public List<BikeRentalStation> stations = new ArrayList<BikeRentalStation>();

    public Map<String, List<RentalUpdaterError>> errorsByNetwork;

    public Map<String, SystemInformation.SystemInformationData> systemInformationDataByNetwork;
}
