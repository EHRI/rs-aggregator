FROM java:8

# Install maven
RUN apt-get update
RUN apt-get install -y maven

WORKDIR /code

# Prepare by downloading dependencies
ADD pom.xml /code/pom.xml
RUN ["mvn", "dependency:resolve"]
RUN ["mvn", "verify"]

# Adding source, compile and package into a fat jar
ADD src /code/src
ADD cfg /code/cfg
RUN ["mvn", "package"]

# Remove source
RUN rm -R /code/src

# Expose directories
VOLUME /code/cfg \
       /code/logs \
       /code/destination

# Execute the application upon container start
CMD ["/usr/lib/jvm/java-8-openjdk-amd64/bin/java", "-jar", "target/rs-aggregator-jar-with-dependencies.jar"]