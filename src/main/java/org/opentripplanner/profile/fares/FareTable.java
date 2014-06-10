package org.opentripplanner.profile.fares;

import com.csvreader.CsvReader;
import com.google.common.collect.Maps;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.profile.DCFareCalculator.Fare;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * Stop names are just strings. Only works on fares from a single feed.
 */
public class FareTable {

    private static final Logger LOG = LoggerFactory.getLogger(FareTable.class);

    private Map<P2<String>, Fare> fares = Maps.newHashMap();

    public FareTable (String name) {
        InputStream is = FareTable.class.getClassLoader().getResourceAsStream(name);
        CsvReader reader = new CsvReader(is, ',', Charset.forName("UTF-8"));
        try {
            reader.readHeaders();
            while (reader.readRecord()) {
                String from = reader.get("from_stop_id");
                String to = reader.get("to_stop_id");
                double low = Double.parseDouble(reader.get("low_fare"));
                double peak = Double.parseDouble(reader.get("peak_fare"));
                double senior = Double.parseDouble(reader.get("senior_fare"));
                Fare fare = new Fare(low, peak, senior);
                fares.put(new P2<String>(from, to), fare);
            }
        } catch (IOException ex) {
            LOG.error("Exception while loading fare table CSV.");
        }
    }

    public Fare lookup (String from, String to) {
        return new Fare(fares.get(new P2<String>(from, to))); // defensive copy, in case the caller discounts
    }

    public Fare lookup (Stop from, Stop to) {
        return lookup(from.getId().getId(), to.getId().getId());
    }

}
