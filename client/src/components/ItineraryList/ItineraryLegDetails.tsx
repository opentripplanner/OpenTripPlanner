import { Leg, Mode } from '../../gql/graphql.ts';
import { LegTime } from './LegTime.tsx';
import { formatDistance } from '../../util/formatDistance.ts';
import { formatDuration } from '../../util/formatDuration.ts';
import { InterchangeInfo } from './InterchangeInfo.tsx';

export function ItineraryLegDetails({ leg, isLast }: { leg: Leg; isLast: boolean }) {
  return (
    <div className="itinerary-leg-details">
      <div className="times">
        {formatDistance(leg.distance)}, {formatDuration(leg.duration)}
      </div>
      <InterchangeInfo leg={leg} />
      <LegTime aimedTime={leg.aimedStartTime} expectedTime={leg.expectedStartTime} hasRealtime={leg.realtime} /> -{' '}
      <LegTime aimedTime={leg.aimedEndTime} expectedTime={leg.expectedEndTime} hasRealtime={leg.realtime} />
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
        {leg.mode !== Mode.Foot && (
          <>
            <br />
            <u title={leg.fromPlace.quay?.id}>{leg.fromPlace.name}</u> →{' '}
          </>
        )}{' '}
        {!isLast && <u title={leg.toPlace.quay?.id}>{leg.toPlace.name}</u>}
      </div>
    </div>
  );
}
