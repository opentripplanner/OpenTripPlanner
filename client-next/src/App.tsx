import Container from 'react-bootstrap/Container';
import Navbar from 'react-bootstrap/Navbar';

export function App() {
  return (
    <Navbar expand="lg" className="bg-body-tertiary">
      <Container>
        <Navbar.Brand href="#home">OTP debug client (next)</Navbar.Brand>
      </Container>
    </Navbar>
  );
}
