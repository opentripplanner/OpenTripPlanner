# Health API

## Contact Info
- Entur, Norway

## Changelog
- Initial implementation of readiness endpoint (November 2019)

## Documentation
This provides endpoints for checking the health status of the OTP instance. It can be useful when 
running OTP in a container.

The API will be at the endpoint http://localhost:8080/otp/actuators and follows the Spring Boot
actuator API standard.
 
### Configuration
To enable this you need to add the feature `ActuatorAPI`.
