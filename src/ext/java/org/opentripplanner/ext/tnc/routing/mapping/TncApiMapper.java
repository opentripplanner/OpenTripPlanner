package org.opentripplanner.ext.tnc.routing.mapping;

import org.opentripplanner.ext.tnc.api.model.TransportationNetworkCompanySummary;
import org.opentripplanner.ext.tnc.routing.TransportationNetworkCompanyService;
import org.opentripplanner.model.plan.RideOffer;
import org.opentripplanner.routing.core.Money;

public final class TncApiMapper {

    /** This is autility class with only static methods; Hence the private constructor. */
    private TncApiMapper() { }


    public static TransportationNetworkCompanySummary mapData(RideOffer domain) {
        if(domain == null) {
            return null;
        }

        TransportationNetworkCompanySummary api = new TransportationNetworkCompanySummary();
        api.company = TransportationNetworkCompanyService.getCompany(domain.operator);
        api.travelDuration = domain.travelDurationSeconds;
        api.currency = mapCurrency(domain.maxPrice == null ? domain.minPrice : domain.maxPrice);
        api.maxCost = mapCost(domain.maxPrice);
        api.minCost = mapCost(domain.maxPrice);
        api.productId = domain.productId;
        api.displayName = domain.displayName;
        api.estimatedArrival = domain.estimatedArrival;
        return api;
    }

    private static String mapCurrency(Money domain) {
        return domain.getCurrency().getCurrencyCode();
    }

    private static double mapCost(Money domain) {
        return domain == null
                ? 0d
                : ((double)domain.getCents()) / domain.getCurrency().getDefaultFractionDigits();
    }
}
