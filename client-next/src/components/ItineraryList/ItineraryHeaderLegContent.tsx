import { Leg } from '../../gql/graphql.ts';
import { useHeaderLegContentStyleCalculations } from './useHeaderLegContentStyleCalculations.ts';

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
          maskImage: `url(/debug-client-preview/img/mode/${leg.mode.toLowerCase()}.png)`,
          WebkitMaskImage: `url(/debug-client-preview/img/mode/${leg.mode.toLowerCase()}.png)`,
        }}
      ></div>
      {showPublicCode && <span className="itinerary-header-leg-public-code">{leg.line?.publicCode}</span>}
    </div>
  );
}
