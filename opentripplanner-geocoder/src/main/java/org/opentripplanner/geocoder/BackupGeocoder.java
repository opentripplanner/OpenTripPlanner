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

package org.opentripplanner.geocoder;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Multiplexe two geocoders: a master and a backup.
 * 
 * Try to get results from the master, if no result is found, or an error occurs, switch to the
 * backup. One can chain multiple backup by using again a multiplexer as the backup.
 */
public class BackupGeocoder implements Geocoder {

    private Geocoder masterGeocoder;

    private Geocoder backupGeocoder;

    public BackupGeocoder(Geocoder masterGeocoder, Geocoder backupGeocoder) {
        this.masterGeocoder = masterGeocoder;
        this.backupGeocoder = backupGeocoder;
    }

    @Override
    public GeocoderResults geocode(String address, Envelope bbox) {
        GeocoderResults retval = masterGeocoder.geocode(address, bbox);
        if (retval.getCount() == 0 || retval.getError() != null) {
            retval = backupGeocoder.geocode(address, bbox);
        }
        return retval;
    }
}
