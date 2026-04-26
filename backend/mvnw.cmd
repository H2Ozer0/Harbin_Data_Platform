@echo off
setlocal
@set WRAPPER_JAR="%~dp0.mvn\wrapper\maven-wrapper.jar"
java -cp %WRAPPER_JAR% org.apache.maven.wrapper.MavenWrapperMain %*
