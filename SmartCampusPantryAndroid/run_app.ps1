# Automatically set JAVA_HOME to Android Studio's JBR (Java 21) to avoid compatibility errors with Java 25
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"

Write-Host "Java environment set. Starting build..." -ForegroundColor Green
.\gradlew installDebug
