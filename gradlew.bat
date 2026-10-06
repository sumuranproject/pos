@ECHO OFF
SET DIR=%~dp0
java -cp "%DIR%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
