package org.opentripplanner.geocoder;

import org.locationtech.jts.geom.Envelope;

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
