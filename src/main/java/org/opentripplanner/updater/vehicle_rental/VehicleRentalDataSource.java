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

package org.opentripplanner.updater.vehicle_rental;

import org.opentripplanner.routing.vehicle_rental.VehicleRentalRegion;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.updater.RentalUpdaterError;
import org.opentripplanner.updater.vehicle_rental.GBFSMappings.SystemInformation;

import java.util.List;

/**
 * An interface for modeling a vehicle rental data source. All vehicle rental updaters are assumed to have stations and
 * regions that can be updated over time.
 */
public interface VehicleRentalDataSource {
    /**
     * @return a list of all errors that occurred during the most recent update.
     */
    List<RentalUpdaterError> getErrors();

    /**
     * @return a list of all currently known vehicle rental stations. The updater will use this to update the Graph.
     */
    List<VehicleRentalStation> getStations();

    /**
     * @return a list of all currently known vehicle rental regions. The updater will use this to update the Graph.
     */
    List<VehicleRentalRegion> getRegions();

    /** returns true if the regions have been updated since the last updated */
    boolean regionsUpdated();

    /**
     * Return the System Information found during the most recent update.
     */
    SystemInformation.SystemInformationData getSystemInformation();

    // updates to the latest data
    void update();
}
