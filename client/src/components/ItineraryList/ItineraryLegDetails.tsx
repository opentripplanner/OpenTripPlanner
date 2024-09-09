import { Leg, Mode } from '../../gql/graphql.ts';
import { LegTime } from './LegTime.tsx';
import { formatDistance } from '../../util/formatDistance.ts';
import { formatDuration } from '../../util/formatDuration.ts';
import { InterchangeInfo } from './InterchangeInfo.tsx';
import { quayQueryAsString } from '../../static/query/quayQuery.tsx';
import { lineQueryAsString } from '../../static/query/lineQuery.tsx';
const graphiQLUrl = import.meta.env.VITE_GRAPHIQL_URL;

export function ItineraryLegDetails({ leg, isLast }: { leg: Leg; isLast: boolean }) {
  const lineID = { id: leg.line?.id };
  const fromPlaceID = { id: leg.fromPlace.quay?.id };
  const toPlaceID = { id: leg.toPlace.quay?.id };
  const formattedQuayQuery = encodeURIComponent(quayQueryAsString);
  const formattedLineQuery = encodeURIComponent(lineQueryAsString);
  const formattedLineID = encodeURIComponent(JSON.stringify(lineID));
  const formattedFromPlaceID = encodeURIComponent(JSON.stringify(fromPlaceID));
  const formattedToPlaceID = encodeURIComponent(JSON.stringify(toPlaceID));

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
            <a
              title={leg.line?.id}
              target={'_blank'}
              rel={'noreferrer'}
              href={graphiQLUrl + '&query=' + formattedLineQuery + '&variables=' + formattedLineID}
            >
              {leg.line.publicCode} {leg.toEstimatedCall?.destinationDisplay?.frontText}
            </a>
            , {leg.authority?.name}
          </>
        )}{' '}
        {leg.mode !== Mode.Foot && (
          <>
            <br />
            <a
              title={leg.fromPlace.quay?.id}
              target={'_blank'}
              rel={'noreferrer'}
              href={graphiQLUrl + '&query=' + formattedQuayQuery + '&variables=' + formattedFromPlaceID}
            >
              {leg.fromPlace.name}
            </a>{' '}
            →{' '}
          </>
        )}{' '}
        {!isLast && (
          <a
            title={leg.toPlace.quay?.id}
            target={'_blank'}
            rel={'noreferrer'}
            href={graphiQLUrl + '&query=' + formattedQuayQuery + '&variables=' + formattedToPlaceID}
          >
            {leg.toPlace.name}
          </a>
        )}
      </div>
    </div>
  );
}
