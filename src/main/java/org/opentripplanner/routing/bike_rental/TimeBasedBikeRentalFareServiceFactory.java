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

package org.opentripplanner.routing.bike_rental;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;

import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.impl.DefaultFareServiceFactory;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.services.FareServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

public class TimeBasedBikeRentalFareServiceFactory implements FareServiceFactory {

    private static Logger log = LoggerFactory
            .getLogger(TimeBasedBikeRentalFareServiceFactory.class);

    private FareServiceFactory nextFactory;

    // Each entry is <max time, cents at that time>; the list is sorted in
    // ascending time order
    private List<P2<Integer>> pricingBySecond;

    private Currency currency;

    @Override
    public FareService makeFareService() {
        return new TimeBasedBikeRentalFareService(nextFactory.makeFareService(), currency,
                pricingBySecond);
    }

    @Override
    public void processGtfs(GtfsRelationalDao dao) {
        nextFactory.processGtfs(dao);
    }

    @Override
    public void configure(JsonNode config) {

        // Next fare service
        // TODO Extract to dedicated composite class
        nextFactory = DefaultFareServiceFactory.fromConfig(config.path("next"));

        // Currency
        String currencyStr = config.path("currency").asText(null);
        // There is no "safe" default, so bail-out if missing
        if (currencyStr == null)
            throw new IllegalArgumentException("Missing mandatory 'currency' configuration.");
        // The following line will throw an IllegalArgumentException if the currency is not found
        currency = Currency.getInstance(currencyStr);

        // List of {time, price_cents}
        pricingBySecond = new ArrayList<>();
        for (JsonNode pConfig : config.path("prices")) {
            int maxTimeSec = hmsToSeconds(pConfig.get("maxTime").asText());
            int priceCent = pConfig.get("priceCents").asInt();
            pricingBySecond.add(new P2<>(maxTimeSec, priceCent));
        }
        if (pricingBySecond.isEmpty())
            throw new IllegalArgumentException("Missing or empty mandatory 'prices' array.");
        // Sort on increasing time
        Collections.sort(pricingBySecond, new Comparator<P2<Integer>>() {
            @Override
            public int compare(P2<Integer> o1, P2<Integer> o2) {
                return o1.first - o2.first;
            }
        });
        // Check if price is increasing
        int seconds = -1;
        int lastCost = 0;
        for (P2<Integer> bracket : pricingBySecond) {
            int maxTime = bracket.first;
            int cost = bracket.second;
            if (maxTime == seconds) {
                throw new IllegalArgumentException("Bike share pricing has two entries for "
                        + maxTime);
            }
            if (cost < lastCost) {
                log.warn("Bike share pricing has pathological pricing; keeping the bike for a "
                        + maxTime + "  is cheaper than keeping it for " + seconds);
            }
            seconds = maxTime;
            lastCost = cost;
        }
    }

    private int hmsToSeconds(String hmsStr) {
        String[] hms = hmsStr.split(":");
        int seconds = 0;
        for (String field : hms) {
            seconds *= 60;
            int fieldValue = Integer.parseInt(field);
            seconds += fieldValue;
        }
        return seconds;
    }
}
