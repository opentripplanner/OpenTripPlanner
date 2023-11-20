import { formatTime } from '../../util/formatTime.ts';

export function LegTime({ aimedTime, expectedTime }: { aimedTime: string; expectedTime: string }) {
  return aimedTime !== expectedTime ? (
    <>
      <span style={{ color: 'red' }}>{formatTime(expectedTime)}</span>
      <span style={{ textDecoration: 'line-through' }}>{formatTime(aimedTime)}</span>
    </>
  ) : (
    <span>{formatTime(expectedTime)}</span>
  );
}
