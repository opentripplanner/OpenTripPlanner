import { Leg, Mode } from '../../gql/graphql.ts';
import { LegTime } from './LegTime.tsx';
import { formatDistance } from '../../util/formatDistance.ts';
import { formatDuration } from '../../util/formatDuration.ts';
import logo from '../../static/img/stay-seated.svg';
import React from 'react';

const staySeatedIcon: (leg: Leg) => React.JSX.Element = (leg: Leg) => {
  if (leg.interchangeFrom?.staySeated) {
    return (
      <img
        alt="Stay-seated transfer"
        title="Stay-seated transfer"
        src={logo}
        width="20"
        height="20"
        className="d-inline-block align-middle"
      />
    );
  } else {
    return <></>;
  }
};

export function ItineraryLegDetails({ leg, isLast }: { leg: Leg; isLast: boolean }) {
  return (
    <div className="itinerary-leg-details">
      <div className="times">
        {formatDistance(leg.distance)}, {formatDuration(leg.duration)}
      </div>
      <div>
        {staySeatedIcon(leg)}
        <LegTime
          aimedTime={leg.aimedStartTime}
          expectedTime={leg.expectedStartTime}
          hasRealtime={leg.realtime}
        /> - <LegTime aimedTime={leg.aimedEndTime} expectedTime={leg.expectedEndTime} hasRealtime={leg.realtime} />
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
        {leg.mode !== Mode.Foot && (
          <>
            <u title={leg.fromPlace.quay?.id}>{leg.fromPlace.name}</u> →{' '}
          </>
        )}{' '}
        {!isLast && <u title={leg.toPlace.quay?.id}>{leg.toPlace.name}</u>}
      </div>
    </div>
  );
}
