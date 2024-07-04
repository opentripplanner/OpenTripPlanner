import { TripPattern } from '../../gql/graphql.ts';
import { ItineraryLegDetails } from './ItineraryLegDetails.tsx';

export function ItineraryDetails({ tripPattern }: { tripPattern: TripPattern }) {
  return (
    <div>
      {tripPattern.systemNotices.length > 0 && (
        <p>System tags: {tripPattern.systemNotices.map((systemNotice) => systemNotice.tag).join(', ')}</p>
      )}
      {tripPattern.legs.map((leg, i) => (
        <ItineraryLegDetails key={leg.id ? leg.id : `noid_${i}`} leg={leg} isLast={i === tripPattern.legs.length - 1} />
      ))}
    </div>
  );
}
