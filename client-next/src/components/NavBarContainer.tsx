import Navbar from 'react-bootstrap/Navbar';
export function NavBarContainer() {
  return (
    <Navbar expand="lg" data-bs-theme="light">
      <div className="navbar-container">
        <Navbar.Brand>
          <img alt="" src="/img/otp-logo.svg" width="30" height="30" className="d-inline-block align-top" /> OTP Debug
          Client
        </Navbar.Brand>
      </div>
    </Navbar>
  );
}
