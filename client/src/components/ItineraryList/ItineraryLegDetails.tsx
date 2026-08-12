import { Mode } from '../../gql/graphql.ts';
import { LegTime } from './LegTime.tsx';
import { formatDistance } from '../../util/distanceUtils.ts';
import { formatDuration } from '../../util/formatDuration.ts';
import { InterchangeInfo } from './InterchangeInfo.tsx';
import { ItineraryGraphiQLLineLink } from './ItineraryGraphiQLLineLink.tsx';
import { ItineraryGraphiQLQuayLink } from './ItineraryGraphiQLQuayLink.tsx';
import { ItineraryGraphiQLAuthorityLink } from './ItineraryGraphiQLAuthorityLink.tsx';
import { ItineraryGraphiQLBikeRentalStationLink } from './ItineraryGraphiQLBikeRentalStationLink.tsx';
import { Leg } from '../../static/query/tripQueryTypes';

/**
 * Some GTFS trips don't have a short name (public code) so we use the long name in this case.
 */
function legName(leg: Leg): string {
  if (leg.line?.publicCode) {
    return leg.line.publicCode + ' ' + leg.fromEstimatedCall?.destinationDisplay?.frontText;
  } else {
    return leg.line?.name || 'unknown';
  }
}
type Place = Leg['fromPlace'];

/** GBFS enum values are snake_case, e.g. scooter_standing, cargo_bicycle, electric_assist. */
function readable(value: string | undefined | null): string | undefined {
  return value?.toLowerCase().replace(/_/g, ' ');
}

/**
 * What you collect here, rather than what the operator happens to call it. A free-floating
 * vehicle's place name is the GBFS vehicle type name, which is free text: the same network yields
 * "E-scooter" for a scooter and a model number such as "Explorer_5" for a bike, neither of which
 * says it is a rental or whose it is.
 *
 * A station keeps its own name, which is a real place, with the network added.
 */
function placeName(place: Place, withNetwork: boolean): string | undefined {
  const vehicle = place.rentalVehicle;
  if (vehicle) {
    // "human" propulsion is the unremarkable case and only lengthens the line.
    const propulsion = readable(vehicle.vehicleType?.propulsionType);
    const kind = [propulsion === 'human' ? undefined : propulsion, readable(vehicle.vehicleType?.formFactor)]
      .filter(Boolean)
      .join(' ');
    const description = kind ? `${kind} rental` : 'rental';
    return withNetwork && vehicle.network ? `${description} (${vehicle.network})` : description;
  }

  const station = place.bikeRentalStation;
  if (station) {
    const name = place.name ?? station.name;
    const networks = withNetwork ? placeNetwork(place) : undefined;
    return networks ? `${name} (${networks})` : (name ?? undefined);
  }

  return place.name ?? undefined;
}

/** The rental network a place belongs to, whether it holds a vehicle or is a station. */
function placeNetwork(place: Place): string | undefined {
  return place.rentalVehicle?.network ?? place.bikeRentalStation?.networks?.filter(Boolean).join(', ') ?? undefined;
}

/**
 * You rent from one network for the whole leg, so it belongs to the leg rather than to each end of
 * it. Undefined on a leg that is not itself the rental - a walk to the vehicle has nowhere else to
 * put it, so there the place name carries it.
 */
function legNetwork(leg: Leg): string | undefined {
  return placeNetwork(leg.fromPlace);
}

/**
 * A rental leg's places carry either a free-floating vehicle or a station, and never a quay, so
 * linking to the quay query produced a link with no variables.
 */
function PlaceLink({ place, withNetwork }: { place: Place; withNetwork: boolean }) {
  const name = placeName(place, withNetwork);
  if (place.quay?.id) {
    return <ItineraryGraphiQLQuayLink legId={place.quay.id} legName={name} />;
  }
  if (place.bikeRentalStation?.id) {
    return <ItineraryGraphiQLBikeRentalStationLink stationId={place.bikeRentalStation.id} legName={name} />;
  }
  // Transmodel has no root query for a free-floating vehicle, so there is nothing to link to.
  return <>{name}</>;
}

export function ItineraryLegDetails({ leg, isLast }: { leg: Leg; isLast: boolean }) {
  const network = legNetwork(leg);
  return (
    <div className="itinerary-leg-details">
      <div className="times">
        {formatDistance(leg.distance)}, {formatDuration(leg.duration)},{' '}
        <span title={'Generalized cost: ¢' + leg.generalizedCost}>¢{leg.generalizedCost}</span>
      </div>
      <InterchangeInfo leg={leg} />
      <LegTime aimedTime={leg.aimedStartTime} expectedTime={leg.expectedStartTime} hasRealtime={leg.realtime} /> -{' '}
      <LegTime aimedTime={leg.aimedEndTime} expectedTime={leg.expectedEndTime} hasRealtime={leg.realtime} />
      <div className="mode">
        <b>{leg.mode}</b> {network && <span className="rental-network">{network}</span>}{' '}
        {leg.line && (
          <>
            <ItineraryGraphiQLLineLink legId={leg.line?.id} legName={legName(leg)} />
            , <ItineraryGraphiQLAuthorityLink legId={leg.authority?.id} legName={leg.authority?.name} />
          </>
        )}{' '}
        {leg.mode !== Mode.Foot && (
          <>
            <br />
            <PlaceLink place={leg.fromPlace} withNetwork={!network} /> →{' '}
          </>
        )}{' '}
        {!isLast && <PlaceLink place={leg.toPlace} withNetwork={!network} />}
      </div>
    </div>
  );
}
