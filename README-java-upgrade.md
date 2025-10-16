Upgrade to Java 21 (manual steps)

This repository targets Java 21. The automated upgrade tools were unavailable in this environment, so follow these manual steps to prepare your machine and validate the build.

1) Install a Java 21 JDK (macOS - Zsh)

Recommended: Adoptium/Eclipse Temurin or SDKMAN.

Using Homebrew (Temurin):

```bash
brew install temurin@21
sudo ln -sfn $(/usr/libexec/java_home -v21) /Library/Java/JavaVirtualMachines/temurin-21.jdk
```

Using SDKMAN:

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21-tem
sdk use java 21-tem
```

2) Verify Java 21 is available

```bash
java -version
# should show a 21.x runtime

/usr/libexec/java_home -v21
# returns the path to JDK 21
```

3) Gradle

This project uses the Gradle wrapper (`gradlew`) with Gradle 9.x which supports Java 21. Gradle will prefer the configured toolchain in `build.gradle.kts`.

To run a build that uses the toolchain:

```bash
./gradlew --version
./gradlew clean assembleDebug --no-daemon
```

If Gradle cannot find a local JDK 21, it will attempt to use an installed JDK. Ensure `JAVA_HOME` points to the JDK 21 installation if necessary:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v21)
```

4) Notes & Troubleshooting

- The project already sets `Versions.JAVA` to `JavaVersion.VERSION_21` in `buildSrc/src/main/kotlin/Versions.kt`.
- If compilation fails with Kotlin-related errors, ensure Kotlin plugin (`kotlin` in `gradle/libs.versions.toml`) is recent. Current repo uses Kotlin 2.2.x which is compatible.
- If you want me to try automatic JDK installation or apply further code changes (toolchains per module, kotlinOptions jvmTarget), tell me and I'll proceed.

