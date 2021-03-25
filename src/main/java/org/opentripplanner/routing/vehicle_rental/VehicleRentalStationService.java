/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.vehicle_rental;

import org.opentripplanner.updater.RentalUpdaterError;
import org.opentripplanner.updater.vehicle_rental.GBFSMappings.SystemInformation;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A service to help link between the vehicle rental updaters and API requests and graph searches. This class holds
 * information about vehicle rental stations and vehicle rental regions from all vehicle updaters.
 */
public class VehicleRentalStationService implements Serializable {
    private static final long serialVersionUID = -1288992939159246764L;

    /* vehicleRentalRegions is a map of vehicle network name to its service area. */
    private Map<String, VehicleRentalRegion> vehicleRentalRegions = new HashMap<>();

    private Set<VehicleRentalStation> vehicleRentalStations = new HashSet<VehicleRentalStation>();

    /* A map of vehicle network name to the latest errors encountered while fetching the feed */
    private Map<String, List<RentalUpdaterError>> errorsByNetwork = new HashMap<>();

    /* A map of vehicle network name to the latest system information data received while fetching the feed */
    private Map<String, SystemInformation.SystemInformationData> systemInformationDataByNetwork = new HashMap<>();

    public Collection<VehicleRentalStation> getVehicleRentalStations() {
        return vehicleRentalStations;
    }

    public void addVehicleRentalStation(VehicleRentalStation vehicleRentalStation) {
        // Remove old reference first, as adding will be a no-op if already present
        vehicleRentalStations.remove(vehicleRentalStation);
        vehicleRentalStations.add(vehicleRentalStation);
    }

    public void removeVehicleRentalStation(VehicleRentalStation vehicleRentalStation) {
        vehicleRentalStations.remove(vehicleRentalStation);
    }

    public Map<String, VehicleRentalRegion> getVehicleRentalRegions() {
        return vehicleRentalRegions;
    }
    
    public void addVehicleRentalRegion(VehicleRentalRegion vehicleRentalRegion) {
        vehicleRentalRegions.put(vehicleRentalRegion.network, vehicleRentalRegion);
    }

    public Map<String, List<RentalUpdaterError>> getErrorsByNetwork() {
        return errorsByNetwork;
    }

    public void setErrorsForNetwork(String network, List<RentalUpdaterError> errors) {
        errorsByNetwork.put(network, errors);
    }

    public Map<String, SystemInformation.SystemInformationData> getSystemInformationDataByNetwork() {
        return systemInformationDataByNetwork;
    }

    public void setSystemInformationDataForNetwork(
        String network,
        SystemInformation.SystemInformationData systemInformationData
    ) {
        systemInformationDataByNetwork.put(network, systemInformationData);
    }
}
