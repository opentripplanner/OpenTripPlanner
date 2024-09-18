import { formatTime } from '../../util/formatTime.ts';

export function LegTime({
  aimedTime,
  expectedTime,
  hasRealtime,
}: {
  aimedTime: string;
  expectedTime: string;
  hasRealtime: boolean;
}) {
  return aimedTime !== expectedTime ? (
    <>
      <span title={expectedTime} style={{ color: 'red' }}>
        {formatTime(expectedTime, 'short')}
      </span>
      <span title={aimedTime} style={{ textDecoration: 'line-through' }}>
        {formatTime(aimedTime, 'short')}
      </span>
    </>
  ) : (
    <span title={expectedTime}>
      {formatTime(expectedTime, 'short')}
      {hasRealtime && <span> (on time)</span>}
    </span>
  );
}
