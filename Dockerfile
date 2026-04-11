
cat > /e/tramtinhieuvutru/backend/Dockerfile << 'EOF'

WORKDIR /app

COPY tram/pom.xml .

COPY tram/src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

