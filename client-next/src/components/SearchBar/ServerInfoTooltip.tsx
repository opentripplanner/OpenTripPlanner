import { ServerInfo } from '../../gql/graphql.ts';
import { Overlay } from 'react-bootstrap';
import { MutableRefObject, ReactNode, useRef } from 'react';

export function ServerInfoTooltip({ target, serverInfo }: { target: MutableRefObject<any>; serverInfo?: ServerInfo }) {
  return (
    <Overlay target={target.current} show={!!serverInfo} placement="right">
      {({
        placement: _placement,
        arrowProps: _arrowProps,
        show: _show,
        popper: _popper,
        hasDoneInitialMeasure: _hasDoneInitialMeasure,
        ...props
      }) => (
        <div
          {...props}
          style={{
            position: 'absolute',
            backgroundColor: 'rgba(255, 100, 100, 0.85)',
            padding: '2px 10px',
            color: 'white',
            borderRadius: 3,
            ...props.style,
          }}
        >
          <pre>{JSON.stringify(serverInfo, null, 2)}</pre>
        </div>
      )}
    </Overlay>
  );
}
