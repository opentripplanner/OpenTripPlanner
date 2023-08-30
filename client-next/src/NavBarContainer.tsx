import Container from 'react-bootstrap/Container';
import Navbar from 'react-bootstrap/Navbar';
export function NavBarContainer() {
  return (
    <Navbar expand="lg" className="bg-body-tertiary">
      <Container>
        <Navbar.Brand>OTP debug client (next)</Navbar.Brand>
      </Container>
    </Navbar>
  );
}
