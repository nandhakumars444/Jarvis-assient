@rem Gradle wrapper for Windows
@rem Just open the project folder in Android Studio instead.
@echo off
set GRADLE_WRAPPER_JAR=gradle\wrapper\gradle-wrapper.jar

if exist "%GRADLE_WRAPPER_JAR%" (
  java -jar "%GRADLE_WRAPPER_JAR%" %*
  goto :end
)

where gradle >nul 2>&1
if %ERRORLEVEL%==0 (
  gradle %*
  goto :end
)

echo Gradle wrapper jar not found.
echo Please open this project in Android Studio.
echo Android Studio will download Gradle automatically.
exit /b 1
:end
