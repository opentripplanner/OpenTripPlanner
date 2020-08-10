# Security

OTP's built-in Grizzly server is configured to accept HTTPS connections on port 8081 by default, but the HTTPS listener needs an encryption key to establish a connection. The key is placed in a "keystore", a format specific to Java server environments. 

## Creating a keystore 

By default, OTP will look for the keystore at `/var/otp/keystore`. To generate a self-signed key for testing, use the command:

    keytool -genkey -keystore /var/otp/keystore -alias OTPServerKey

The alias of the key is arbitrary, but it's best to supply one that indicates the purpose of the key to override the default. `keytool` will ask you a series of questions about you and your organization; again, any values will do when creating this self-signed test key. `keytool` will also ask you for a password to protect your keystore and key. This password will eventually be configurable, but for now it is hard-coded into the OTP server, so you must set the keystore and key passwords both to `opentrip`.

Of course with a self-signed key, most clients will (rightfully) refuse to connect without special permission from the user. You'll need to add a security exception to most web browsers, or add the `--insecure` switch when using CURL. You could theoretically buy and install a "real" trusted SSL/TLS certificate it in the keystore using `keytool -gencert`, but since none of the functionality protected by this encryption is public-facing a self-signed key should be sufficient for most use cases. All connections to these API methods should be from trusted parties who can verify the validity of the key with you directly as needed.

## Testing

Once you have created a key, start up the OTP server and test that HTTPS access and authentication are possible. The following command should trigger a reload of all graphs:

    curl -v --insecure -X PUT --user ROUTERS:ultra_secret -H "accept: application/json" "https://localhost:8081/otp/routers"

The username and password (`ROUTERS:ultra_secret`) are placeholders hard-coded into OTP. They will be configurable in version 1.0.

You should also be able to fetch any other OTP resources over HTTPS. For example, you could open a raw TLS connection using `openssl s_client -connect localhost:8081`, then issue the request `GET index.html HTTP/1.1`.

## CORS

TODO explain this
