import { useState, useRef } from 'react';
import Navbar from 'react-bootstrap/Navbar';
import { ServerInfo } from '../../gql/graphql.ts';
import { ServerInfoTooltip } from './ServerInfoTooltip.tsx';
import logo from '../../static/img/otp-logo.svg';

type LogoSectionProps = {
  serverInfo?: ServerInfo;
};

export function LogoSection({ serverInfo }: LogoSectionProps) {
  const [showServerInfo, setShowServerInfo] = useState(false);
  const target = useRef(null);

  return (
    <div className="logo-container box" style={{ display: 'flex' }}>
      <Navbar.Brand onClick={() => setShowServerInfo((v) => !v)}>
        <div ref={target}>
          <img alt="" src={logo} width="50" height="50" className="logo-image" />
          OTP Debug
          {showServerInfo && <ServerInfoTooltip serverInfo={serverInfo} target={target} />}
        </div>
      </Navbar.Brand>
      <div className="details">
        <div>Version: {serverInfo?.version}</div>
        <div>Time zone: {serverInfo?.internalTransitModelTimeZone}</div>
      </div>
    </div>
  );
}
