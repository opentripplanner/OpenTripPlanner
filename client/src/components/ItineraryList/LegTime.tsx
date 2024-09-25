import { formatTime } from '../../util/formatTime.ts';
import { useContext } from 'react';
import { TimeZoneContext } from '../../hooks/TimeZoneContext.ts';

export function LegTime({
  aimedTime,
  expectedTime,
  hasRealtime,
}: {
  aimedTime: string;
  expectedTime: string;
  hasRealtime: boolean;
}) {
  const timeZone = useContext(TimeZoneContext);
  return aimedTime !== expectedTime ? (
    <>
      <span title={expectedTime} style={{ color: 'red' }}>
        {formatTime(expectedTime, timeZone, 'short')}
      </span>
      <span title={aimedTime} style={{ textDecoration: 'line-through' }}>
        {formatTime(aimedTime, timeZone, 'short')}
      </span>
    </>
  ) : (
    <span title={expectedTime}>
      {formatTime(expectedTime, timeZone, 'short')}
      {hasRealtime && <span> (on time)</span>}
    </span>
  );
}
