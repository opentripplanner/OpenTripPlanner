package org.opentripplanner.profile;

import com.google.common.collect.Lists;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.profile.fares.FareTable;

import java.util.List;

/**
 */
public class DCFareCalculator {

    private static final FareTable METRORAIL = new FareTable("org/opentripplanner/profile/fares/metrorail.csv");
    private static final FareTable MARC = new FareTable("org/opentripplanner/profile/fares/marc.csv");
    private static final FareTable VRE = new FareTable("org/opentripplanner/profile/fares/vre.csv");

    private static final String[] metroExpress = { "J7", "J9", "P17", "P19", "W13", "W19", "11Y", "17A", "17B", "17G",
        "17H", "17K", "17L", "17M", "18E", "18G", "18H", "18P", "29E", "29G", "29H", "29X" };

    private static RideType classify (Ride ride) {
        // NOTE the agencyId string of the route's agencyAndId is not the same as the one given by route.getAgency.
        // The former is the same for all routes in the feed. The latter is the true agency of the feed.
        String agency = ride.route.getAgency().getId();
        String agency_url = ride.route.getAgency().getUrl(); // this is used in single-agency feeds so it should work
        String short_name = ride.route.getShortName();
        String long_name = ride.route.getLongName();
        if ("MET".equals(agency)) {
            if (ride.route.getType() == 1) return RideType.METRO_RAIL;
            if ("5A".equals(short_name) || "B30".equals(short_name)) return RideType.METRO_BUS_AIRPORT;
            for (String sn : metroExpress) if (sn.equals(short_name)) return RideType.METRO_BUS_EXPRESS;
            return RideType.METRO_BUS_LOCAL;
        } else if ("DC".equals(agency)) {
            return RideType.DC_CIRCULATOR_BUS;
        } else if ("MCRO".equals(agency)) {
            if (short_name.equals("70")) return RideType.MCRO_BUS_EXPRESS;
            else return RideType.MCRO_BUS_LOCAL;
        } else if (agency_url != null) {
            if (agency_url.contains("fairfaxconnector.com")) {
                return RideType.FAIRFAX_CONNECTOR_BUS;
            }
            if (agency_url.contains("prtctransit.org")) {
                return RideType.PRTC_BUS;
            }
            if (agency_url.contains("arlingtontransit.com")) {
                return RideType.ART_BUS;
            }
            if (agency_url.contains("vre.org")) {
                return RideType.VRE_BUS;
            }
            if (agency_url.contains("mtamaryland.com")) {
                if (ride.route.getType() == 2) return RideType.MARC_RAIL;
                else return RideType.DASH_BUS; // FIXME this is probably wrong
            }
        }
        return null;
    }

    /**
     * Should we have exactly one fare per ride, where some fares may have zero cost if they are transfers from the same operator?
     * ...except that this doesn't work for MetroRail, where two legs combine into one.
     */
    public static List<Fare> calculateFares (List<Ride> rides) {
        List<FareRide> fareRides = Lists.newArrayList();
        FareRide prev = null;
        for (Ride ride : rides) {
            FareRide fareRide = new FareRide(ride, prev);
            if (prev != null && prev.type == fareRide.type) {
                prev.to = fareRide.to;
                prev.calcFare(); // recalculate existing fare using new destination
            } else {
                fareRides.add(fareRide);
                prev = fareRide;
            }
        }
        List<Fare> fares = Lists.newArrayList();
        for (FareRide fareRide : fareRides) {
            fares.add(fareRide.fare);
        }
        return fares;
    }

    static class FareRide {
        Stop from;
        Stop to;
        Route route;
        RideType type;
        Fare fare;
        FareRide prev;
        public FareRide (Ride ride, FareRide prevRide) {
            from = ride.from;
            to = ride.to;
            route = ride.route;
            type = classify(ride);
            prev = prevRide;
            calcFare();
        }
        private void setFare(double base, boolean transferReduction) {
            fare = new Fare(base);
            fare.transferReduction = transferReduction;
        }
        private void setFare(double low, double peak, double senior, boolean transferReduction) {
            fare = new Fare(peak);
            fare.low = low;
            fare.senior = senior;
            fare.transferReduction = transferReduction;
        }
        // TODO store rule-based Fares in a table keyed on (type, prevtype) instead of doing on the fly
        // automatically compose string using 'free' or 'discounted' and route name
        private void calcFare() {
            RideType prevType = (prev == null) ? null : prev.type;
            switch (type) {
            case METRO_RAIL:
                fare = METRORAIL.lookup(from, to);
                if (prevType == RideType.METRO_BUS_LOCAL || prevType == RideType.METRO_BUS_EXPRESS || // TODO merge local and express categories
                    prevType == RideType.MCRO_BUS_LOCAL  || prevType == RideType.MCRO_BUS_EXPRESS) {  // TODO merge local and express categories
                    fare.discount(0.50);
                }
                break;
            case METRO_BUS_LOCAL:
                if (prevType == RideType.DASH_BUS) {
                    setFare(0.00, true);
                } else if (prevType == RideType.METRO_BUS_EXPRESS || prevType == RideType.METRO_BUS_AIRPORT) {
                    setFare(0.00, true);
                } else if (prevType == RideType.MCRO_BUS_LOCAL || prevType == RideType.MCRO_BUS_EXPRESS) {
                    setFare(0.00, true);
                } else if (prevType == RideType.METRO_RAIL) {
                    setFare(1.10, true);
                } else if (prevType == RideType.ART_BUS) {
                    setFare(0.10, true);
                } else {
                    setFare(1.60, false);
                }
                break;
            case METRO_BUS_EXPRESS:
                if (prevType == RideType.METRO_BUS_LOCAL) {
                    setFare(2.05, true);
                } else {
                    setFare(3.65, false);
                }
                break;
            case METRO_BUS_AIRPORT:
                setFare(6.00, false);
                break;
            case DC_CIRCULATOR_BUS :
                if (prevType == RideType.METRO_BUS_LOCAL || prevType == RideType.METRO_BUS_EXPRESS ||
                    prevType == RideType.METRO_BUS_AIRPORT || prevType == RideType.ART_BUS) {
                    setFare(0.00, true);
                } else if (prevType == RideType.METRO_RAIL) {
                    setFare(0.50, true);
                } else {
                    setFare(1.00, false);
                }
                break;
            case ART_BUS:
                if (prevType == RideType.METRO_BUS_LOCAL || prevType == RideType.METRO_BUS_EXPRESS) {
                    setFare(0.00, true);
                } else if (prevType == RideType.METRO_RAIL) {
                    setFare(1.00, true);
                } else {
                    setFare(1.50, false);
                }
                break;
            case DASH_BUS:
                if (prevType == RideType.METRO_BUS_LOCAL || prevType == RideType.METRO_BUS_EXPRESS) {
                    setFare(0.00, true);
                } else {
                    setFare(1.60, false);
                }
                break;
            case MARC_RAIL:
                fare = MARC.lookup(from, to);
                break;
            case VRE_BUS:
                fare = VRE.lookup(from, to);
                break;
            case MCRO_BUS_LOCAL:
                if (prevType == RideType.MCRO_BUS_EXPRESS) {
                    setFare(0.00, true);
                } else if (prevType == RideType.METRO_RAIL) {
                    setFare(1.10, true);
                } else {
                    setFare(1.60, false);
                }
                break;
            case MCRO_BUS_EXPRESS:
                if (prevType == RideType.MCRO_BUS_LOCAL) {
                    setFare(2.05, true);
                } else if (prevType == RideType.METRO_RAIL) {
                    setFare(3.15, true);
                } else {
                    setFare(3.65, false);
                }
                break;
            case FAIRFAX_CONNECTOR_BUS:
                String routeName = route.getShortName();
                if (routeName.equals("394") || routeName.equals("395")) {
                    setFare(3.65, false);
                } else if (routeName.equals("480")) {
                    setFare(5.00, false);
                } else if (routeName.equals("595") || routeName.equals("597")) {
                    setFare(7.50, false);
                }
                break;
            case PRTC_BUS:
                routeName = route.getLongName();
                if (prevType == RideType.VRE_BUS) {
                    setFare(0.00, true);
                } else if (routeName.contains("omniride")) {
                    setFare(5.75, false);
                } else if (routeName.contains("omnilink") || routeName.contains("connector")) {
                    setFare(1.30, false);
                } else if (routeName.contains("metro direct")) {
                    setFare(2.90, false);
                }
                break;
            default:
                setFare(0.00, false);
            }
            if (fare != null) fare.type = type;
        }
    }

    public static class Fare {

        public RideType type;
        public double low;
        public double peak;
        public double senior;
        public boolean transferReduction;

        public Fare (Fare other) {
            this.accumulate(other);
        }

        public Fare (double base) {
            low = peak = senior = base;
        }

        public Fare (double low, double peak, double senior) {
            this.low = low;
            this.peak = peak;
            this.senior = senior;
        }

        public void accumulate (Fare other) {
            if (other != null) {
                low    += other.low;
                peak   += other.peak;
                senior += other.senior;
            }
        }

        public void discount(double amount) {
            low    -= amount;
            peak   -= amount;
            senior -= amount;
            transferReduction = true;
        }

    }

    enum RideType {
        METRO_RAIL,
        METRO_BUS_LOCAL,
        METRO_BUS_EXPRESS,
        METRO_BUS_AIRPORT,
        DC_CIRCULATOR_BUS,
        ART_BUS,
        DASH_BUS,
        MARC_RAIL,
        VRE_BUS,
        MCRO_BUS_LOCAL,
        MCRO_BUS_EXPRESS,
        FAIRFAX_CONNECTOR_BUS,
        PRTC_BUS
    }

}
