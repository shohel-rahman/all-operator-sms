FROM openjdk:17
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
EXPOSE 9597
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.config.location=E:/config/teletalk/"]