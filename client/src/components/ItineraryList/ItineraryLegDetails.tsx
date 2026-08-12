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

/** GBFS form factors are snake_case, e.g. scooter_standing, cargo_bicycle. */
function readableFormFactor(formFactor: string | undefined): string | undefined {
  return formFactor?.toLowerCase().replace(/_/g, ' ');
}

/**
 * What you collect here, rather than what the operator happens to call it. A free-floating
 * vehicle's place name is the GBFS vehicle type name, which is free text: the same network yields
 * "E-scooter" for a scooter and a model number such as "Explorer_5" for a bike, neither of which
 * says it is a rental or whose it is.
 */
function placeName(place: Place): string | undefined {
  const vehicle = place.rentalVehicle;
  if (vehicle) {
    const kind = readableFormFactor(vehicle.vehicleType?.formFactor) ?? 'vehicle';
    return vehicle.network ? `${kind} rental (${vehicle.network})` : `${kind} rental`;
  }
  return place.name ?? undefined;
}

/**
 * A rental leg's places carry either a free-floating vehicle or a station, and never a quay, so
 * linking to the quay query produced a link with no variables.
 */
function PlaceLink({ place }: { place: Place }) {
  const name = placeName(place);
  if (place.quay?.id) {
    return <ItineraryGraphiQLQuayLink legId={place.quay.id} legName={name} />;
  }
  if (place.bikeRentalStation?.id) {
    return <ItineraryGraphiQLBikeRentalStationLink stationId={place.bikeRentalStation.id} legName={name} />;
  }
  // Transmodel has no root query for a free-floating vehicle, so there is nothing to link to.
  return <>{name}</>;
}

/**
 * Which operator's vehicle this is, and what kind - neither of which the mode alone tells you when
 * several networks serve the same area.
 */
function rentalDescription(leg: Leg): string | undefined {
  const vehicle = leg.fromPlace.rentalVehicle;
  if (vehicle) {
    const formFactor = readableFormFactor(vehicle.vehicleType?.formFactor);
    const propulsion = vehicle.vehicleType?.propulsionType?.toLowerCase();
    const kind = [formFactor, propulsion && `(${propulsion})`].filter(Boolean).join(' ');
    return [vehicle.network, kind].filter(Boolean).join(' · ');
  }
  const station = leg.fromPlace.bikeRentalStation;
  if (station) {
    return [station.networks?.filter(Boolean).join(', '), 'station'].filter(Boolean).join(' · ');
  }
  return undefined;
}

export function ItineraryLegDetails({ leg, isLast }: { leg: Leg; isLast: boolean }) {
  const rental = rentalDescription(leg);
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
        <b>{leg.mode}</b> {rental && <span className="rental-info">{rental}</span>}{' '}
        {leg.line && (
          <>
            <ItineraryGraphiQLLineLink legId={leg.line?.id} legName={legName(leg)} />
            , <ItineraryGraphiQLAuthorityLink legId={leg.authority?.id} legName={leg.authority?.name} />
          </>
        )}{' '}
        {leg.mode !== Mode.Foot && (
          <>
            <br />
            <PlaceLink place={leg.fromPlace} /> →{' '}
          </>
        )}{' '}
        {!isLast && <PlaceLink place={leg.toPlace} />}
      </div>
    </div>
  );
}
