FROM eclipse-temurin:21-jre

# Définir le fuseau horaire du conteneur
ENV TZ=Europe/Berlin

# Dossier principal
WORKDIR /app

# Copier le JAR shaded
COPY otp-shaded/target/otp-shaded-2.7.0.jar /app/otp-shaded.jar

# Créer le dossier OTP pour les données
RUN mkdir -p /var/opentripplanner && chmod 755 /var/opentripplanner

# Exposer les ports OTP
EXPOSE 8080 8081

# Volume de données persistantes
VOLUME ["/var/opentripplanner"]

# Point d'entrée direct vers Java avec les JAVA_TOOL_OPTIONS prises en compte
ENTRYPOINT ["java", "-jar", "/app/otp-shaded.jar", "/var/opentripplanner"]

# Commande par défaut (peut être écrasée dans docker-compose)
CMD ["--load", "--serve"]

