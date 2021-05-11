package org.opentripplanner.updater.vehicle_parking;

import static java.util.Locale.ROOT;

import java.util.List;
import java.util.Locale;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.util.xml.XmlDataListDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Load bike park from a KML placemarks. Use name as bike park name and point coordinates. Rely on:
 * 1) bike park to be KML Placemarks, 2) geometry to be Point.
 * 
 * Bike park-and-ride and "OV-fiets mode" development has been funded by GoAbout
 * (https://goabout.com/).
 * 
 * @author laurent
 * @author GoAbout
 */
class KmlBikeParkDataSource implements VehicleParkingDataSource {

    private static final Logger LOG = LoggerFactory.getLogger(KmlBikeParkDataSource.class);

    private final String url;

    private final String feedId;

    private final String namePrefix;

    private final boolean zip;

    private final XmlDataListDownloader<VehicleParking> xmlDownloader;

    private List<VehicleParking> bikeParks;

    public KmlBikeParkDataSource(Parameters config) {
        this.url = config.getUrl();
        this.feedId = config.getFeedId();
        this.namePrefix = config.getNamePrefix();
        this.zip = config.zip();

        xmlDownloader = new XmlDataListDownloader<>();
        xmlDownloader
                .setPath("//*[local-name()='kml']/*[local-name()='Document']/*[local-name()='Placemark']|//*[local-name()='kml']/*[local-name()='Document']/*[local-name()='Folder']/*[local-name()='Placemark']");
        xmlDownloader.setDataFactory(attributes -> {
            if (!attributes.containsKey("name")) {
                LOG.warn("Missing name in KML Placemark, cannot create bike park.");
                return null;
            }
            if (!attributes.containsKey("Point")) {
                LOG.warn("Missing Point geometry in KML Placemark, cannot create bike park.");
                return null;
            }

            var name = (namePrefix != null ? namePrefix : "") + attributes.get("name").trim();
            String[] coords = attributes.get("Point").trim().split(",");
            var x = Double.parseDouble(coords[0]);
            var y = Double.parseDouble(coords[1]);
            var id = String.format(
                    ROOT, "%s[%.3f-%.3f]",
                    name.replace(" ", "_"),
                    x, y
            );

            var localizedName = new NonLocalizedString(name);

            return VehicleParking.builder()
                .name(localizedName)
                .x(x)
                .y(y)
                .id(new FeedScopedId(feedId, id))
                .entrances(List.of(
                    VehicleParking.VehicleParkingEntrance.builder()
                        .entranceId(new FeedScopedId(feedId, id))
                        .name(localizedName)
                        .x(x)
                        .y(y)
                        .build()
                ))
                .build();
        });
    }

    /**
     * Update the data from the source;
     * 
     * @return true if there might have been changes
     */
    @Override
    public boolean update() {
        List<VehicleParking> newBikeParks = xmlDownloader.download(url, zip);
        if (newBikeParks != null) {
            synchronized (this) {
                // Update atomically
                bikeParks = newBikeParks;
            }
            return true;
        }
        return false;
    }

    @Override
    public synchronized List<VehicleParking> getVehicleParkings() {
        return bikeParks;
    }

    @Override
    public String toString() {
        return getClass().getName() + "(" + url + ")";
    }

    public interface Parameters {
        String getUrl();
        String getFeedId();
        String getNamePrefix();
        boolean zip();
    }
}
