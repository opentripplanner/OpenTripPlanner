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
      <span style={{ color: 'red' }}>{formatTime(expectedTime)}</span>
      <span style={{ textDecoration: 'line-through' }}>{formatTime(aimedTime)}</span>
    </>
  ) : (
    <span>
      {formatTime(expectedTime)}
      {hasRealtime && <span> (on time)</span>}
    </span>
  );
}
