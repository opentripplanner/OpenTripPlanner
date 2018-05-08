FROM openjdk:8

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

RUN mkdir -p base/graphs/dc

# Add DC data.
ADD coord/otp-base/graphs/dc/* base/graphs/dc/

# Add fake locations.
RUN mkdir -p coord/test-gbfs
ADD coord/test-gbfs/* coord/test-gbfs/

# Add router.
ADD target/otp-1.3.0-SNAPSHOT-shaded.jar otp-1.3.0-SNAPSHOT-shaded.jar


# Expose the backend.
EXPOSE 8080

# TODO(danieljy): Figure out why loading a pre-built graph yields errors.
# For loading a pre-built graph:
# CMD java -Xmx4G -jar otp-1.3.0-SNAPSHOT-shaded.jar --server --basePath base --router dc

# Build and load the graph from raw data.
CMD java -Xmx4G -jar otp-1.3.0-SNAPSHOT-shaded.jar --build base/graphs/dc --inMemory