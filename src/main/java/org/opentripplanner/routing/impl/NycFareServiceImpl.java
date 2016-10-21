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

package org.opentripplanner.routing.impl;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.WrappedCurrency;
import org.opentripplanner.routing.edgetype.DwellEdge;
import org.opentripplanner.routing.edgetype.HopEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.spt.GraphPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Currency;
import java.util.LinkedList;
import java.util.List;

enum NycFareState {
	INIT, 
	SUBWAY_PRE_TRANSFER,
	SUBWAY_PRE_TRANSFER_WALKED,
	SUBWAY_POST_TRANSFER,
	SIR_PRE_TRANSFER, 
	SIR_POST_TRANSFER_FROM_SUBWAY,
	SIR_POST_TRANSFER_FROM_BUS, 
	EXPENSIVE_EXPRESS_BUS, 
	BUS_PRE_TRANSFER, CANARSIE,
}

/**
 * This handles the New York City MTA's baroque fare rules for subways and buses
 * with the following limitations:
 * (1) the two hour limit on transfers is not enforced
 * (2) the b61/b62 special case is not handled
 * (3) MNR, LIRR, and LI Bus are not supported -- only subways and buses   
 */
public class NycFareServiceImpl implements FareService, Serializable {
        private static final Logger LOG = LoggerFactory.getLogger(NycFareServiceImpl.class);

	private static final long serialVersionUID = 1L;

	private static final float ORDINARY_FARE = 2.25f;

	private static final float EXPRESS_FARE = 5.50f;

	private static final float EXPENSIVE_EXPRESS_FARE = 7.50f; // BxM4C only

	public NycFareServiceImpl() {
	}

	@Override
	public Fare getCost(GraphPath path) {

		final List<AgencyAndId> SIR_PAID_STOPS = makeMtaStopList("S31", "S30");

		final List<AgencyAndId> SUBWAY_FREE_TRANSFER_STOPS = makeMtaStopList(
				"R11", "B08", "629");

		final List<AgencyAndId> SIR_BONUS_STOPS = makeMtaStopList("140", "420",
				"419", "418", "M22", "M23", "R27", "R26");

		final List<AgencyAndId> SIR_BONUS_ROUTES = makeMtaStopList("M5", "M20",
				"M15-SBS");

		final List<AgencyAndId> CANARSIE = makeMtaStopList("L29", "303345");

		// List of NYC agencies to set fares for
		final List<String> AGENCIES = new ArrayList<>();
		AGENCIES.add("MTABC");
		AGENCIES.add("MTA NYCT");

		LinkedList<State> states = path.states;

		// create rides
		List<Ride> rides = new ArrayList<Ride>();
		Ride newRide = null;
		final int SUBWAY = 1;
		final int SIR = 2;
		final int LOCAL_BUS = 3;
		final int EXPRESS_BUS = 30;
		final int EXPENSIVE_EXPRESS_BUS = 34;
		final int WALK = -1;

		for (State state : states) {

			Edge backEdge = state.getBackEdge();
			if (backEdge instanceof StreetEdge) {
				if (newRide == null || !newRide.classifier.equals(WALK)) {
					if (rides.size() == 0 || !rides.get(rides.size() - 1).classifier.equals(WALK)) {
						newRide = new Ride();
						newRide.classifier = WALK;
						rides.add(newRide);
					}
				}
				continue;
			}

			// dwells do not affect fare.
			if (backEdge instanceof DwellEdge)
				continue;

			if (!(backEdge instanceof HopEdge)) {
				newRide = null;
				continue;
			}
			AgencyAndId routeId = state.getRoute();
			String agencyId = state.getBackTrip().getRoute().getAgency().getId();
			if (!AGENCIES.contains(agencyId)) {
				continue;
			}
			if (routeId == null) {
				newRide = null;
			} else {
				if (newRide == null || !routeId.equals(newRide.route)) {
					newRide = new Ride();
					rides.add(newRide);

					newRide.firstStop = ((HopEdge) backEdge).getBeginStop();

					newRide.route = routeId;
					Trip trip = state.getBackTrip();
					Route route = trip.getRoute();
					int type = route.getType();
					newRide.classifier = type;
					String shortName = route.getShortName();
					if (shortName == null ) {
						newRide.classifier = SUBWAY;
					} else if (shortName.equals("BxM4C")) {
						newRide.classifier = EXPENSIVE_EXPRESS_BUS;
					} else if (shortName.startsWith("X")
							|| shortName.startsWith("BxM")
							|| shortName.startsWith("QM")
							|| shortName.startsWith("BM")) {
						newRide.classifier = EXPRESS_BUS; // Express bus
					} 

					newRide.startTime = state.getTimeSeconds();
				}
				newRide.lastStop = ((HopEdge) backEdge).getBeginStop();
			}
		}

		// There are no rides, so there's no fare.
		if (rides.size() == 0) {
			return null;
		}

		NycFareState state = NycFareState.INIT;
		boolean lexFreeTransfer = false;
		boolean canarsieFreeTransfer = false;
		boolean siLocalBus = false;
		boolean sirBonusTransfer = false;
		float totalFare = 0;
		for (Ride ride : rides) {
			AgencyAndId firstStopId = null;
			AgencyAndId lastStopId = null; 
			if (ride.firstStop != null) {
				firstStopId = ride.firstStop.getId();
				lastStopId = ride.lastStop.getId();
			}
			switch (state) {
			case INIT:
				lexFreeTransfer = siLocalBus = canarsieFreeTransfer = false;
				if (ride.classifier.equals(WALK)) {
					// walking keeps you in init
				} else if (ride.classifier.equals(SUBWAY)) {
					state = NycFareState.SUBWAY_PRE_TRANSFER;
					totalFare += ORDINARY_FARE;
					if (SUBWAY_FREE_TRANSFER_STOPS.contains(ride.lastStop.getId())) {
						lexFreeTransfer = true;
					}
					if (CANARSIE.contains(ride.lastStop.getId())) {
						canarsieFreeTransfer = true;
					}
				} else if (ride.classifier.equals(SIR)) {
					state = NycFareState.SIR_PRE_TRANSFER;
					if (SIR_PAID_STOPS.contains(firstStopId)
							|| SIR_PAID_STOPS.contains(lastStopId)) {
						totalFare += ORDINARY_FARE;
					}
				} else if (ride.classifier.equals(LOCAL_BUS)) {
					state = NycFareState.BUS_PRE_TRANSFER;
					totalFare += ORDINARY_FARE;
					if (CANARSIE.contains(ride.lastStop.getId())) {
						canarsieFreeTransfer = true;
					}
					siLocalBus = ride.route.getId().startsWith("S");
				} else if (ride.classifier.equals(EXPRESS_BUS)) {
					state = NycFareState.BUS_PRE_TRANSFER;
					totalFare += EXPRESS_FARE;
				} else if (ride.classifier.equals(EXPENSIVE_EXPRESS_BUS)) {
					state = NycFareState.EXPENSIVE_EXPRESS_BUS;
					totalFare += EXPENSIVE_EXPRESS_FARE;
				}
				break;
			case SUBWAY_PRE_TRANSFER_WALKED:
				if (ride.classifier.equals(SUBWAY)) {
					// subway-to-subway transfers are verbotten except at
					// lex and 59/63
					if (!(lexFreeTransfer && SUBWAY_FREE_TRANSFER_STOPS
							.contains(ride.firstStop.getId()))) {
						totalFare += ORDINARY_FARE;
					}

					lexFreeTransfer = canarsieFreeTransfer = false;
					if (SUBWAY_FREE_TRANSFER_STOPS.contains(ride.lastStop.getId())) {
						lexFreeTransfer = true;
					}
					if (CANARSIE.contains(ride.lastStop.getId())) {
						canarsieFreeTransfer = true;
					}
				}
				/* FALL THROUGH */
			case SUBWAY_PRE_TRANSFER:
				// it will always be possible to transfer from the first subway
				// trip to anywhere,
				// since no sequence of subway trips takes greater than two
				// hours (if only just)
				if (ride.classifier.equals(WALK)) {
					state = NycFareState.SUBWAY_PRE_TRANSFER_WALKED;
				} else if (ride.classifier.equals(SIR)) {
					state = NycFareState.SIR_POST_TRANSFER_FROM_SUBWAY;
				} else if (ride.classifier.equals(LOCAL_BUS)) {

					if (CANARSIE.contains(ride.firstStop.getId())
							&& canarsieFreeTransfer) {
						state = NycFareState.BUS_PRE_TRANSFER;
					} else {
						state = NycFareState.INIT;
					}
				} else if (ride.classifier.equals(EXPRESS_BUS)) {
					// need to pay the upgrade cost
					totalFare += EXPRESS_FARE - ORDINARY_FARE;
				} else if (ride.classifier.equals(EXPENSIVE_EXPRESS_BUS)) {
					totalFare += EXPENSIVE_EXPRESS_FARE; // no transfers to the
					// BxMM4C
				}
				break;
			case BUS_PRE_TRANSFER:
				if (ride.classifier.equals(SUBWAY)) {
					if (CANARSIE.contains(ride.firstStop.getId())
							&& canarsieFreeTransfer) {
						state = NycFareState.SUBWAY_PRE_TRANSFER;
					} else {
						state = NycFareState.INIT;
					}
				} else if (ride.classifier.equals(SIR)) {
					if (siLocalBus) {
						// SI local bus to SIR, so it is as if we started on the
						// SIR (except that when we enter the bus or subway system we need to do
						// so at certain places)
						sirBonusTransfer = true;
						state = NycFareState.SIR_PRE_TRANSFER;
					} else {
						//transfers exhausted
						state = NycFareState.INIT;
					}
				} else if (ride.classifier.equals(LOCAL_BUS)) {
					state = NycFareState.INIT;
				} else if (ride.classifier.equals(EXPRESS_BUS)) {
					// need to pay the upgrade cost
					totalFare += EXPRESS_FARE - ORDINARY_FARE;
					state = NycFareState.INIT;
				} else if (ride.classifier.equals(EXPENSIVE_EXPRESS_BUS)) {
					totalFare += EXPENSIVE_EXPRESS_FARE; 
					// no transfers to the BxMM4C
				}
				
				break;
			case SIR_PRE_TRANSFER:
				if (ride.classifier.equals(SUBWAY)) {
					if (sirBonusTransfer && !SIR_BONUS_STOPS.contains(ride.firstStop.getId())) {
						//we were relying on the bonus transfer to be in the "pre-transfer state",
						//but the bonus transfer does not apply here
						totalFare += ORDINARY_FARE;
					}
					if (CANARSIE.contains(ride.lastStop.getId())) {
						canarsieFreeTransfer = true;
					}
					state = NycFareState.SUBWAY_POST_TRANSFER;
				} else if (ride.classifier.equals(SIR)) {
					/* should not happen, and unhandled */
					LOG.warn("Should not transfer from SIR to SIR");
				} else if (ride.classifier.equals(LOCAL_BUS)) {
					if (!SIR_BONUS_ROUTES.contains(ride.route)) {
						totalFare += ORDINARY_FARE;
					}
					state = NycFareState.BUS_PRE_TRANSFER;
				} else if (ride.classifier.equals(EXPRESS_BUS)) {
					totalFare += EXPRESS_BUS;
					state = NycFareState.BUS_PRE_TRANSFER;
				} else if (ride.classifier.equals(EXPENSIVE_EXPRESS_BUS)) {
					totalFare += EXPENSIVE_EXPRESS_BUS;
					state = NycFareState.BUS_PRE_TRANSFER;
				}
				break;
			case SIR_POST_TRANSFER_FROM_SUBWAY:
				if (ride.classifier.equals(SUBWAY)) {
					/* should not happen */
					totalFare += ORDINARY_FARE;
					state = NycFareState.SUBWAY_PRE_TRANSFER;
				} else if (ride.classifier.equals(SIR)) {
					/* should not happen, and unhandled */
					LOG.warn("Should not transfer from SIR to SIR");
				} else if (ride.classifier.equals(LOCAL_BUS)) {
					if (!ride.route.getId().startsWith("S")) {
						totalFare += ORDINARY_FARE;
						state = NycFareState.BUS_PRE_TRANSFER;
					} else {
						state = NycFareState.INIT;
					}
				} else if (ride.classifier.equals(EXPRESS_BUS)) {
					// need to pay the full cost
					totalFare += EXPRESS_FARE;
					state = NycFareState.INIT;
				} else if (ride.classifier.equals(EXPENSIVE_EXPRESS_BUS)) {
					/* should not happen */
					// no transfers to the BxMM4C
					totalFare += EXPENSIVE_EXPRESS_FARE;
					state = NycFareState.BUS_PRE_TRANSFER;
				}
				break;
		    case SUBWAY_POST_TRANSFER:
		    	if (ride.classifier.equals(WALK)) {
		    		if (!canarsieFreeTransfer) { 
			    		/* note: if we end up walking to another subway after alighting
			    		 * at Canarsie, we will mistakenly not be charged, but nobody
			    		 * would ever do this */
		    			state = NycFareState.INIT;
		    		}
		    	} else if (ride.classifier.equals(SIR)) {
		    		totalFare += ORDINARY_FARE;
		    		state = NycFareState.SIR_PRE_TRANSFER;
		    	} else if (ride.classifier.equals(LOCAL_BUS)) {
		    		if (!(CANARSIE.contains(ride.firstStop.getId())
		    				&& canarsieFreeTransfer)) {
		    			totalFare += ORDINARY_FARE;
		    		}
	    			state = NycFareState.INIT;
		    	} else if (ride.classifier.equals(SUBWAY)) {
					//walking transfer
					totalFare += ORDINARY_FARE;
					state = NycFareState.SUBWAY_PRE_TRANSFER;
				} else if (ride.classifier.equals(EXPRESS_BUS)) {
					totalFare += EXPRESS_FARE;
					state = NycFareState.BUS_PRE_TRANSFER;
				} else if (ride.classifier.equals(EXPENSIVE_EXPRESS_BUS)) {
					totalFare += EXPENSIVE_EXPRESS_FARE;
					state = NycFareState.BUS_PRE_TRANSFER;
				} 
			}
		}

		Currency currency = Currency.getInstance("USD");
		Fare fare = new Fare();
		fare.addFare(FareType.regular, new WrappedCurrency(currency),
				(int) Math.round(totalFare
						* Math.pow(10, currency.getDefaultFractionDigits())));
		return fare;
	}

	private List<AgencyAndId> makeMtaStopList(String... stops) {

		ArrayList<AgencyAndId> out = new ArrayList<AgencyAndId>();
		for (String stop : stops) {
			out.add(new AgencyAndId("MTA NYCT", stop));
			out.add(new AgencyAndId("MTA NYCT", stop + "N"));
			out.add(new AgencyAndId("MTA NYCT", stop + "S"));
		}
		return out;
	}

}
