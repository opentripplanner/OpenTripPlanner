import { Leg, Mode } from '../../gql/graphql.ts';
import { LegTime } from './LegTime.tsx';
import { formatDistance } from '../../util/formatDistance.ts';
import { formatDuration } from '../../util/formatDuration.ts';
import { InterchangeInfo } from './InterchangeInfo.tsx';
import { ItineraryGraphiQLLineLink } from './ItineraryGraphiQLLineLink.tsx';
import { ItineraryGraphiQLQuayLink } from './ItineraryGraphiQLQuayLink.tsx';
import { ItineraryGraphiQLAuthorityLink } from './ItineraryGraphiQLAuthorityLink.tsx';

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
            <ItineraryGraphiQLLineLink
              legId={leg.line?.id}
              legName={leg.line.publicCode + ' ' + leg.toEstimatedCall?.destinationDisplay?.frontText}
            />
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
