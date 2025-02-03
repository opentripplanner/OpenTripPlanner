import React from 'react';
import infoIcon from '../../static/img/help-info-solid.svg';
import inputIcon from '../../static/img/input.svg';
import durationIcon from '../../static/img/lap-timer.svg';
import { ResolvedType } from './useTripArgs.ts';

interface ArgumentTooltipProps {
  defaultValue?: string | number | boolean | object | null | undefined;
  type?: ResolvedType;
}

const ArgumentTooltip: React.FC<ArgumentTooltipProps> = ({ defaultValue, type }) => {
  return (
    <span>
      {defaultValue !== undefined && defaultValue !== null && (
        <span title={`Default: ${defaultValue ?? 'None'}`}>
          <img alt={'Info'} src={infoIcon} className="default-tooltip-icon"></img>
        </span>
      )}
      {type !== undefined && type !== null && type.subtype === 'DoubleFunction' && (
        <span title={`Type: ${type.subtype ?? 'None'}\n Format: \`a + b t\`. Example: \`30m + 2.0 t\``}>
          <img alt={'Info'} src={inputIcon} className="default-tooltip-icon"></img>
        </span>
      )}
      {type !== undefined && type !== null && type.subtype === 'Duration' && (
        <span
          className="default-tooltip-icon"
          title={`Type: ${type.subtype ?? 'None'}\n Duration in a lenient ISO-8601 duration format.\nExample P2DT2H12M40S, 2d2h12m40s or 1h`}
        >
          <img alt={'Info'} src={durationIcon} className="default-tooltip-icon"></img>
        </span>
      )}
    </span>
  );
};

export default ArgumentTooltip;
