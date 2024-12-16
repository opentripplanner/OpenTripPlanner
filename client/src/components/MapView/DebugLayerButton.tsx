import debugLayerIcon from '../../static/img/debug-layer.svg';

import { useState } from 'react';

type DebugLayerButtonProps = {
    onToggle: (isExpanded: boolean) => void;
};

export default function DebugLayerButton({ onToggle }: DebugLayerButtonProps) {
    const [isExpanded, setIsExpanded] = useState(false);

    const handleClick = () => {
        const nextState = !isExpanded;
        setIsExpanded(nextState);
        onToggle(nextState); // Notify parent of the state change
    };

    return (
        <button
            style={{
                cursor: 'pointer',
                border: 'none',
                background: 'none',
                textAlign: 'left',
                width: '100%',
                padding: "0",
                margin: "0",
            }}
            onClick={handleClick}
        >
            {isExpanded ? (
                <span style={{ fontWeight: 'bold' }}>Debug Layers</span>
            ) : (
                <img
                    src={debugLayerIcon}
                    alt="Debug layer"
                    title="Debug layer"
                    style={{ width: '25px', height: '25px' }}
                />
            )}
        </button>
    );
}
