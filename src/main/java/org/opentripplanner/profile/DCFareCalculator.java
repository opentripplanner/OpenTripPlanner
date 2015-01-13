package org.opentripplanner.profile;

import com.google.common.collect.Lists;

import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.profile.fares.FareTable;

import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.List;

/**
 */
public class DCFareCalculator {

    private static final FareTable METRORAIL = new FareTable("org/opentripplanner/profile/fares/metrorail.csv");
    private static final FareTable MARC = new FareTable("org/opentripplanner/profile/fares/marc.csv");
    private static final FareTable VRE = new FareTable("org/opentripplanner/profile/fares/vre.csv");

    private static final String[] metroExpress = { "J7", "J9", "P17", "P19", "W13", "W19", "11Y", "17A", "17B", "17G",
        "17H", "17K", "17L", "17M", "18E", "18G", "18H", "18P", "29E", "29G", "29H", "29X" };

    // geographic fare areas for MTA (Maryland) commuter buses
    private static final FareArea MTA_DC = new FareArea(-77.066139,38.868166,-76.986868,38.926);
    private static final FareArea MTA_SILVER_SPRING = new FareArea(-76.943069,38.838617,-76.916848,38.855997);
    private static final FareArea MTA_SUITLAND = new FareArea(-77.04039,38.985653,-77.02017,39.002778);
    private static final FareArea MTA_KENT_ISLAND = new FareArea(-76.360503,38.939413,-76.170122,39.012155);
    private static final FareArea MTA_DAVIDSONVILLE = new FareArea(-76.669367,38.952661,-76.637353,38.970023);
    private static final FareArea MTA_ANNAPOLIS = new FareArea(-76.574267,38.960267,-76.469125,39.001783);
    private static final FareArea MTA_OWINGS_NORTH_BEACH = new FareArea(-76.623115,38.687874,-76.519002,38.732147);
    private static final FareArea MTA_PINDELL = new FareArea(-76.772156,38.754121,-76.639891,38.830921);
    private static final FareArea MTA_CHARLOTTE_HALL_WALDORF = new FareArea(-76.97628,38.435241,-76.71225,38.656712);
    private static final FareArea MTA_CALIFORNIA = new FareArea(-76.571502,38.273603,-76.483254,38.3446);
    private static final FareArea MTA_DUNKIRK = new FareArea(-76.677069, 38.692187, -76.635025, 38.742091);
    private static final FareArea MTA_SUNDERLAND = new FareArea(-76.627974, 38.530956, -76.564987, 38.678062);
    private static final FareArea MTA_ST_LEONARD = new FareArea(-76.527123, 38.458123, -76.48802, 38.495911);
    
    private static RideType classify (Route route) {
        // NOTE the agencyId string of the route's agencyAndId is not the same as the one given by route.getAgency.
        // The former is the same for all routes in the feed. The latter is the true agency of the feed.

        String agency = route.getAgency().getId();
        String agency_url = route.getAgency().getUrl(); // this is used in single-agency feeds so it should work
        String short_name = route.getShortName();
        String long_name = route.getLongName();
        if ("MET".equals(agency)) {
            if (route.getType() == 1) return RideType.METRO_RAIL;
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
                return RideType.VRE_RAIL;
            }
            if (agency_url.contains("mtamaryland.com")) {
                if (route.getType() == 2) return RideType.MARC_RAIL;
                int shortName;
                try {
                	shortName = Integer.parseInt(route.getShortName());
                }
                catch(NumberFormatException ex) {
                	// assume a local bus if route number cannot be parsed
                	return RideType.MTA_BUS_LOCAL;
                }
                if(shortName < 100) { // local routes are 0 - 99
                	return RideType.MTA_BUS_LOCAL;
                }
                else if(shortName < 200) { // express routes are 100 - 199
                	return RideType.MTA_BUS_EXPRESS;
                }
                // commuter routes are 200+
                return RideType.MTA_BUS_COMMUTER;
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
            // Calculate the fare for a ride based on only one of its patterns. This is usually right as long as the
            // patterns are all the same mode and operator. That happens less naturally now that stops are being grouped.
            // FIXME cluster stops considering mode and operator
            PatternRide exemplarPatternRide = ride.patternRides.get(0);
            FareRide fareRide = new FareRide(exemplarPatternRide, prev);
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
    
    public static Rectangle2D.Double createFareZone(double min_lon, double max_lat, double max_lon, double min_lat) {
    	return new Rectangle2D.Double(min_lon, max_lat, max_lon - min_lon, max_lat - min_lat);
    }

    static class FareArea extends Rectangle2D.Double {
    	
    	public FareArea(double min_lon, double min_lat, double max_lon, double max_lat) {
    		super(min_lon, min_lat, max_lon - min_lon, max_lat - min_lat);
    	}
    	
    	public boolean containsStop(Stop stop) {
    		return super.contains(stop.getLon(), stop.getLat());
    	}
    }
    
    static class FareRide {
        Stop from;
        Stop to;
        Route route;
        RideType type;
        Fare fare;
        FareRide prev;
        public FareRide (PatternRide pRide, FareRide prevRide) {
            from = pRide.getFromStop();
            to = pRide.getToStop();
            // Problem: Rides no longer have a single fare because they may be on multiple routes.
            // TODO: make sure Patterns in Rides are all the same mode and operator.
            // This seems to happen naturally because different operators generally do not share stops.
            route = pRide.pattern.route;
            type = classify(route);
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
            if (type == null)
                return;
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
            case VRE_RAIL:
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
                if (prevType == RideType.VRE_RAIL) {
                    setFare(0.00, true);
                } else if (routeName.contains("omniride")) {
                    setFare(5.75, false);
                } else if (routeName.contains("omnilink") || routeName.contains("connector")) {
                    setFare(1.30, false);
                } else if (routeName.contains("metro direct")) {
                    setFare(2.90, false);
                }
                break;
            case MTA_BUS_LOCAL:
                setFare(1.60, false);
                break;            	
            case MTA_BUS_EXPRESS:
                setFare(2.00, false);
                break;            	
            case MTA_BUS_COMMUTER:
            	String shortName = route.getShortName();
            	double mtaDefault = 0;
            	
            	switch(shortName) {
            	
        		// $5.00 flat-fare routes
        		case "201":
        		case "202":
        		case "203":
        		case "204":
        		case "240":
                    setFare(5.00, false);
                    break;
                    
          		// $4.25 flat-fare routes
        		case "230":
        		case "260":
        		case "335":
        		case "345":
        		case "610":
        		case "620":
        		case "630":
        		case "640":
        		case "650":
                    setFare(4.25, false);
        			break;

          		// $3.50 flat-fare routes
        		case "310":
        		case "410":
                    setFare(3.50, false);
        			break;

        		// variable-fare routes
        			
        		case "220":
        			if(MTA_KENT_ISLAND.containsStop(from) || MTA_KENT_ISLAND.containsStop(to)) {
                        setFare(5.00, false);        				
        			}
        			else if(MTA_ANNAPOLIS.containsStop(from) || MTA_ANNAPOLIS.containsStop(to)) {
                        setFare(4.25, false);        				
        			}
        			else {
    	                setFare(mtaDefault, false);
        			}
        			break;

        		case "250":
        			if(MTA_KENT_ISLAND.containsStop(from) || MTA_KENT_ISLAND.containsStop(to)) {
                        setFare(5.00, false);        				
        			}
        			else if(MTA_DAVIDSONVILLE.containsStop(from) || MTA_DAVIDSONVILLE.containsStop(to)) {
                        setFare(4.25, false);        				
        			}
        			else {
    	                setFare(mtaDefault, false);
        			}
        			break; 
        			
        		case "305":
        		case "315":
        		case "325":
        			if(MTA_DC.containsStop(from) || MTA_DC.containsStop(to)) {
                        setFare(4.25, false);        				
        			}
        			else if(MTA_SILVER_SPRING.containsStop(from) || MTA_SILVER_SPRING.containsStop(to)) {
                        setFare(3.50, false);        				
        			}
        			else {
    	                setFare(mtaDefault, false);
        			}
        			break;        			

        		case "902":
        			if(MTA_DUNKIRK.containsStop(from) || MTA_DUNKIRK.containsStop(to)) {
                        setFare(3.50, false);        				
        			}
        			else if(MTA_SUNDERLAND.containsStop(from) || MTA_SUNDERLAND.containsStop(to)) {
                        setFare(4.25, false);        				
        			}
        			else if(MTA_ST_LEONARD.containsStop(from) || MTA_ST_LEONARD.containsStop(to)) {
                        setFare(5.00, false);        				
        			}
        			else {
    	                setFare(mtaDefault, false);
        			}
        			break;

        		case "903":
        			if(MTA_DC.containsStop(from) || MTA_DC.containsStop(to)) {
                        setFare(4.25, false);        				
        			}
        			else if(MTA_SUITLAND.containsStop(from) || MTA_SUITLAND.containsStop(to)) {
                        setFare(3.50, false);        				
        			}
        			else {
    	                setFare(mtaDefault, false);
        			}
        			break;        			

        		case "904":
        			if(MTA_PINDELL.containsStop(from) || MTA_PINDELL.containsStop(to)) {
                        setFare(3.50, false);        				
        			}
        			else if(MTA_OWINGS_NORTH_BEACH.containsStop(from) || MTA_OWINGS_NORTH_BEACH.containsStop(to)) {
                        setFare(4.25, false);        				
        			}
        			else {
    	                setFare(mtaDefault, false);
        			}
        			break;        			

        		case "905":
        		case "909":
        			if(MTA_CHARLOTTE_HALL_WALDORF.containsStop(from) || MTA_CHARLOTTE_HALL_WALDORF.containsStop(to)) {
                        setFare(4.25, false);        				
        			}
        			else if(MTA_CALIFORNIA.containsStop(from) || MTA_CALIFORNIA.containsStop(to)) {
                        setFare(5.75, false);        				
        			}
        			else {
    	                setFare(mtaDefault, false);
        			}
        			break;        			
        			
        		default:
	                setFare(mtaDefault, false);
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
        MTA_BUS_LOCAL,
        MTA_BUS_EXPRESS,
        MTA_BUS_COMMUTER,
        VRE_RAIL,
        MCRO_BUS_LOCAL,
        MCRO_BUS_EXPRESS,
        FAIRFAX_CONNECTOR_BUS,
        PRTC_BUS
    }

}
