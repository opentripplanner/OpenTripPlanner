java -Xmx6G -jar java/app.jar --build otp --save && \
CMD java -Xmx6G -jar java/app.jar --load --port 8080 --securePort 8081 otp
