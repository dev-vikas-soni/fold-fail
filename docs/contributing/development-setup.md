# Development Setup Guide

## Prerequisites
- JDK 21+
- Gradle 8.5+ or use `./gradlew` wrapper

## Building the Project
```bash
git clone https://github.com/dev-vikas-soni/viewlint.git
cd viewlint
./gradlew build
```

## Building ShadowJar
```bash
./gradlew :viewlint-gradle-plugin:shadowJar
```
