import { useMemo } from 'react';
import { isTransitMode } from '../../util/isTransitMode.ts';
import { getColorForLeg } from '../../util/getColorForLeg.ts';
import { generateTextColor } from '../../util/generateTextColor.ts';
import { Leg } from '../../static/query/tripQueryTypes';

export function useHeaderLegContentStyleCalculations(
  leg: Leg,
  earliestStartTime: string | null,
  maxSpan: number,
  startPx: number,
  pxSpan: number,
) {
  const startPct = useMemo(
    () => (new Date(leg.expectedStartTime).getTime() - new Date(earliestStartTime!).getTime()) / maxSpan,
    [leg.expectedStartTime, earliestStartTime, maxSpan],
  );

  const widthPx = useMemo(
    () =>
      (pxSpan * (new Date(leg.expectedEndTime).getTime() - new Date(leg.expectedStartTime).getTime())) / maxSpan - 1,
    [pxSpan, leg, maxSpan],
  );

  const leftPx = startPx + startPct * pxSpan + 1;

  const showPublicCode =
    widthPx > 40 && isTransitMode(leg.mode) && leg.line?.publicCode && leg.line.publicCode.length <= 6;

  const modeColor = getColorForLeg(leg);
  const legTextColor = useMemo(() => generateTextColor(modeColor), [modeColor]);

  return { widthPx, leftPx, legTextColor, modeColor, showPublicCode };
}
