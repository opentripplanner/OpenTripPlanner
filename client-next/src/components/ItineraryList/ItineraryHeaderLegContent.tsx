import { Leg } from '../../gql/graphql.ts';
import { useMemo } from 'react';
import { getColorForMode } from '../../util/getColorForMode.ts';
import { isTransitMode } from '../../util/isTransitMode.ts';
import { generateTextColor } from '../../util/generateTextColor.ts';

export function ItineraryHeaderLegContent({
  leg,
  earliestStartTime,
  maxSpan,
  startPx,
  pxSpan,
}: {
  leg: Leg;
  earliestStartTime: string | null;
  maxSpan: number;
  startPx: number;
  pxSpan: number;
}) {
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

  const modeColor = getColorForMode(leg.mode);
  const legTextColor = useMemo(() => generateTextColor(modeColor), [modeColor]);

  return (
    <div
      style={{
        position: 'absolute',
        width: `${widthPx}px`,
        height: '22px',
        left: leftPx,
        color: legTextColor,
        background: modeColor,
        fontWeight: 'bold',
        textShadow: 'none',
        marginTop: '-1px',
        paddingTop: '3px',
        textAlign: 'center',
        overflow: 'hidden',
      }}
    >
      <div
        style={{
          background: legTextColor,
          maskImage: `url(/img/mode/${leg.mode.toLowerCase()}.png)`,
          maskRepeat: 'no-repeat',
          WebkitMaskImage: `url(/img/mode/${leg.mode.toLowerCase()}.png)`,
          WebkitMaskRepeat: 'no-repeat',
          width: '17px',
          height: '17px',
          display: 'inline-block',
        }}
      ></div>
      {showPublicCode && (
        <span style={{ verticalAlign: 'top', fontSize: '14px', paddingLeft: '2px' }}>{leg.line?.publicCode}</span>
      )}
    </div>
  );
}
