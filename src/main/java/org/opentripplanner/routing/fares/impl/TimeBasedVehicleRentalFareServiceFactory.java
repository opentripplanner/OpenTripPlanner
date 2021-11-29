package org.opentripplanner.routing.fares.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.routing.fares.FareServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

public class TimeBasedVehicleRentalFareServiceFactory implements FareServiceFactory {

    private static Logger log = LoggerFactory
            .getLogger(TimeBasedVehicleRentalFareServiceFactory.class);

    // Each entry is <max time, cents at that time>; the list is sorted in
    // ascending time order
    private List<P2<Integer>> pricingBySecond;

    private Currency currency;

    @Override
    public FareService makeFareService() {
        return new TimeBasedVehicleRentalFareService(currency, pricingBySecond);
    }

    @Override
    public void processGtfs(OtpTransitService transitService) {
        // Nothing to do
    }

    @Override
    public void configure(JsonNode config) {

        // Currency
        String currencyStr = config.path("currency").asText(null);
        // There is no "safe" default, so bail-out if missing
        if (currencyStr == null)
            throw new IllegalArgumentException("Missing mandatory 'currency' configuration.");
        // The following line will throw an IllegalArgumentException if the currency is not found
        currency = Currency.getInstance(currencyStr);

        // List of {time, price_cents}
        pricingBySecond = new ArrayList<>();
        for (Iterator<Map.Entry<String, JsonNode>> i = config.path("prices").fields(); i.hasNext();) {
            Map.Entry<String, JsonNode> kv = i.next();
            int maxTimeSec = hmToMinutes(kv.getKey()) * 60;
            int priceCent = (int) Math.round(kv.getValue().asDouble() * 100);
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

    private int hmToMinutes(String hmStr) {
        String[] hm = hmStr.split(":");
        if (hm.length > 2)
            throw new IllegalArgumentException("Invalid time: '" + hmStr + "'. Must be either 'hh:mm' or 'mm'.");
        int minutes = 0;
        for (String field : hm) {
            minutes *= 60;
            int fieldValue = Integer.parseInt(field);
            minutes += fieldValue;
        }
        return minutes;
    }
}
