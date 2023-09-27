import { TripPattern } from '../../gql/graphql.ts';
import { TIME_WIDTH, useHeaderContentStyleCalculations } from './useHeaderContentStyleCalculations.ts';

export function ItineraryHeaderContent({
  tripPattern,
  itineraryIndex,
  containerWidth,
  earliestStartTime,
  latestEndTime,
}: {
  tripPattern: TripPattern;
  itineraryIndex: number;
  containerWidth: number;
  earliestStartTime: string | null;
  latestEndTime: string | null;
}) {
  const { widthPx, leftPx } = useHeaderContentStyleCalculations(
    tripPattern,
    containerWidth,
    earliestStartTime,
    latestEndTime,
  );

  return (
    <div style={{ position: 'relative' }}>
      <div style={{ position: 'absolute' }}>{itineraryIndex + 1}.</div>
      <div
        style={{
          position: 'absolute',
          width: `${widthPx + 5}px`,
          height: '2px',
          left: `${leftPx - 2}px`,
          top: '9px',
          background: 'black',
        }}
      />
      <div
        style={{
          position: 'absolute',
          left: `${leftPx - TIME_WIDTH}px`,
          background: 'black',
          color: 'white',
        }}
      >
        {new Date(tripPattern.expectedStartTime).toLocaleTimeString('en-US', {
          timeStyle: 'short',
          hourCycle: 'h24',
        })}
      </div>

      <div
        style={{
          position: 'absolute',
          left: `${leftPx + widthPx + 2}px`,
          background: 'black',
          color: 'white',
        }}
      >
        {new Date(tripPattern.expectedEndTime).toLocaleTimeString('en-US', {
          timeStyle: 'short',
          hourCycle: 'h24',
        })}
      </div>
    </div>
  );
}
