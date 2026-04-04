FROM gradle:9.4.1-jdk21

WORKDIR /workspace

CMD ["./gradlew", "bootRun", "--no-daemon"]
