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

package org.opentripplanner.routing.car_rental;

import org.opentripplanner.updater.RentalUpdaterError;
import org.opentripplanner.updater.vehicle_rental.GBFSMappings.SystemInformation;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CarRentalStationService implements Serializable {
    private static final long serialVersionUID = -1288992939159246764L;

    /* carRentalRegions is a map of car network name to its service area. */
    private Map<String, CarRentalRegion> carRentalRegions = new HashMap<>();

    private Set<CarRentalStation> carRentalStations = new HashSet<CarRentalStation>();

    /* A map of car network name to the latest errors encountered while fetching the feed */
    private Map<String, List<RentalUpdaterError>> errorsByNetwork = new HashMap<>();

    /* A map of car network name to the latest system information data received while fetching the feed */
    private Map<String, SystemInformation.SystemInformationData> systemInformationDataByNetwork = new HashMap<>();

    public Collection<CarRentalStation> getCarRentalStations() {
        return carRentalStations;
    }

    public void addCarRentalStation(CarRentalStation carRentalStation) {
        // Remove old reference first, as adding will be a no-op if already present
        carRentalStations.remove(carRentalStation);
        carRentalStations.add(carRentalStation);
    }

    public void removeCarRentalStation(CarRentalStation carRentalStation) {
        carRentalStations.remove(carRentalStation);
    }

    public Map<String, CarRentalRegion> getCarRentalRegions() {
        return carRentalRegions;
    }
    
    public void addCarRentalRegion(CarRentalRegion carRentalRegion) {
        carRentalRegions.put(carRentalRegion.network, carRentalRegion); 
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
