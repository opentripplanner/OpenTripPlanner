import { Mode } from '../../gql/graphql.ts';
import { LegTime } from './LegTime.tsx';
import { formatDistance } from '../../util/formatDistance.ts';
import { formatDuration } from '../../util/formatDuration.ts';
import { InterchangeInfo } from './InterchangeInfo.tsx';
import { ItineraryGraphiQLLineLink } from './ItineraryGraphiQLLineLink.tsx';
import { ItineraryGraphiQLQuayLink } from './ItineraryGraphiQLQuayLink.tsx';
import { ItineraryGraphiQLAuthorityLink } from './ItineraryGraphiQLAuthorityLink.tsx';
import { Leg } from '../../static/query/tripQueryTypes';

/**
 * Some GTFS trips don't have a short name (public code) so we use the long name in this case.
 */
function legName(leg: Leg): string {
  if (leg.line?.publicCode) {
    return leg.line.publicCode + ' ' + leg.toEstimatedCall?.destinationDisplay?.frontText;
  } else {
    return leg.line?.name || 'unknown';
  }
}
export function ItineraryLegDetails({ leg, isLast }: { leg: Leg; isLast: boolean }) {
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
        <b>{leg.mode}</b>{' '}
        {leg.line && (
          <>
            <ItineraryGraphiQLLineLink legId={leg.line?.id} legName={legName(leg)} />
            , <ItineraryGraphiQLAuthorityLink legId={leg.authority?.id} legName={leg.authority?.name} />
          </>
        )}{' '}
        {leg.mode !== Mode.Foot && (
          <>
            <br />
            <ItineraryGraphiQLQuayLink legId={leg.fromPlace.quay?.id} legName={leg.fromPlace.name} /> →{' '}
          </>
        )}{' '}
        {!isLast && <ItineraryGraphiQLQuayLink legId={leg.toPlace.quay?.id} legName={leg.toPlace.name} />}
      </div>
    </div>
  );
}
