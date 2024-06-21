import { Leg, Mode } from '../../gql/graphql.ts';
import { LegTime } from './LegTime.tsx';
import { formatDistance } from '../../util/formatDistance.ts';
import { formatDuration } from '../../util/formatDuration.ts';

export function ItineraryLegDetails({ leg, isLast }: { leg: Leg; isLast: boolean }) {
  return (
    <div className="itinerary-leg-details">
      <div className="times">
        {formatDistance(leg.distance)}, {formatDuration(leg.duration)}
      </div>
      <div>
        <LegTime aimedTime={leg.aimedStartTime} expectedTime={leg.expectedStartTime} hasRealtime={leg.realtime} /> -{' '}
        <LegTime aimedTime={leg.aimedEndTime} expectedTime={leg.expectedEndTime} hasRealtime={leg.realtime} />
      </div>
      <div className="mode">
        <b>{leg.mode}</b>{' '}
        {leg.line && (
          <>
            <u>
              {leg.line.publicCode} {leg.toEstimatedCall?.destinationDisplay?.frontText}
            </u>
            , {leg.authority?.name}
          </>
        )}{' '}
        <div></div>
        {leg.mode !== Mode.Foot && <u>{leg.fromPlace.name}</u>} {!isLast && <u>→ {leg.toPlace.name}</u>}
      </div>
    </div>
  );
}
