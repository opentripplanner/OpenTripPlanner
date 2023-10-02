import { TripPattern } from '../../gql/graphql.ts';
import { TIME_BOX_WIDTH, useHeaderContentStyleCalculations } from './useHeaderContentStyleCalculations.ts';
import { ItineraryHeaderLegContent } from './ItineraryHeaderLegContent.tsx';

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
  const { maxSpan, pxSpan, startPx, widthPx, leftPx } = useHeaderContentStyleCalculations(
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
          left: `${leftPx - TIME_BOX_WIDTH}px`,
          background: 'black',
          color: 'white',
          fontSize: '12px',
          width: '38px',
          height: '15px',
          textAlign: 'center',
          top: 2,
          padding: 1,
        }}
      >
        {new Date(tripPattern.expectedStartTime).toLocaleTimeString('en-US', {
          timeStyle: 'short',
          hourCycle: 'h24',
        })}
      </div>

      {tripPattern.legs.map((leg, i) => (
        <ItineraryHeaderLegContent
          key={leg.id || `leg_${i}`}
          leg={leg}
          earliestStartTime={earliestStartTime}
          startPx={startPx}
          maxSpan={maxSpan}
          pxSpan={pxSpan}
        />
      ))}

      <div
        style={{
          position: 'absolute',
          left: `${leftPx + widthPx + 2}px`,
          background: 'black',
          color: 'white',
          fontSize: '12px',
          width: '38px',
          height: '15px',
          textAlign: 'center',
          top: 2,
          padding: 1,
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
