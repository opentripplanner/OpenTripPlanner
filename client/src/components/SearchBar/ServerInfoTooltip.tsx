import { ServerInfo } from '../../gql/graphql.ts';
import { Overlay } from 'react-bootstrap';
import { MutableRefObject } from 'react';

export function ServerInfoTooltip({ target, serverInfo }: { target: MutableRefObject<null>; serverInfo?: ServerInfo }) {
  return (
    <Overlay target={target.current} show={!!serverInfo} placement="right">
      <div
        style={{
          position: 'absolute',
          backgroundColor: 'rgba(255, 100, 100, 0.85)',
          padding: '2px 10px',
          color: 'white',
          borderRadius: 3,
        }}
      >
        <pre>{JSON.stringify(serverInfo, null, 2)}</pre>
      </div>
    </Overlay>
  );
}
