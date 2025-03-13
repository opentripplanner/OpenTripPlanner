import { useHeaderLegContentStyleCalculations } from './useHeaderLegContentStyleCalculations.ts';
import { Leg } from '../../static/query/tripQueryTypes';
const modeIcons = import.meta.glob('../../static/img/mode/*.png', { query: '?url', import: 'default', eager: true });

function getModeIconUrl(leg: Leg) {
  return modeIcons[`../../static/img/mode/${leg.mode.toLowerCase()}.png`];
}

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
  const { widthPx, leftPx, legTextColor, modeColor, showPublicCode } = useHeaderLegContentStyleCalculations(
    leg,
    earliestStartTime,
    maxSpan,
    startPx,
    pxSpan,
  );

  const legIconImageUrl = getModeIconUrl(leg);

  return (
    <div
      className="itinerary-header-leg-wrapper"
      style={{
        width: `${widthPx}px`,
        left: leftPx,
        color: legTextColor,
        background: modeColor,
      }}
    >
      <div
        className="itinerary-header-leg-icon"
        style={{
          background: legTextColor,
          maskImage: `url(${legIconImageUrl})`,
          WebkitMaskImage: `url(${legIconImageUrl})`,
        }}
      ></div>
      {showPublicCode && <span className="itinerary-header-leg-public-code">{leg.line?.publicCode}</span>}
    </div>
  );
}
