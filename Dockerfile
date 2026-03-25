# 階段 1：使用 Maven 進行編譯
FROM maven:3.8.4-openjdk-17 AS build
COPY . /app
WORKDIR /app
RUN mvn clean package -DskipTests

# 階段 2：執行 Java 程式
FROM eclipse-temurin:17-jdk-alpine
COPY --from=build /app/target/*.jar app.jar

# 讓 Cloud Run 動態偵測 Port
ENV PORT 8080
EXPOSE 8080

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]