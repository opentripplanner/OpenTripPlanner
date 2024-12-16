import React from 'react';

interface DefaultValueTooltipProps {
  defaultValue: any;
}

const DefaultValueTooltip: React.FC<DefaultValueTooltipProps> = ({ defaultValue }) => {
  return (
    <span className="default-tooltip-icon" title={`Default: ${defaultValue ?? 'None'}`}>
      !
    </span>
  );
};

export default DefaultValueTooltip;
