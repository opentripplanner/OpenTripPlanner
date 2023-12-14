import { Leg, Mode } from '../../gql/graphql.ts';
import { LegTime } from './LegTime.tsx';
import { formatDistance } from '../../util/formatDistance.ts';
import { formatDuration } from '../../util/formatDuration.ts';

export function ItineraryLegDetails({ leg, isLast }: { leg: Leg; isLast: boolean }) {
  return (
    <div style={{ border: '1px dotted grey' }}>
      <LegTime aimedTime={leg.aimedStartTime} expectedTime={leg.expectedStartTime} hasRealtime={leg.realtime} />-{' '}
      <LegTime aimedTime={leg.aimedEndTime} expectedTime={leg.expectedEndTime} hasRealtime={leg.realtime} />{' '}
      <b>{leg.mode}</b>{' '}
      {leg.line && (
        <>
          <u>
            {leg.line.publicCode} {leg.toEstimatedCall?.destinationDisplay?.frontText}
          </u>
          , {leg.authority?.name}
        </>
      )}{' '}
      {formatDistance(leg.distance)}, {formatDuration(leg.duration)}
      {leg.mode !== Mode.Foot && <u>from {leg.fromPlace.name}</u>} {!isLast && <u>to {leg.toPlace.name}</u>}
    </div>
  );
}
