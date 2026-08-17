@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%"=="" @echo off
@if "%SEARCH_DIR%"=="" set SEARCH_DIR=%CD%

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Resolve any "." and ".." in APP_HOME to make it shorter and cleaner.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Find gradle-wrapper.jar
if exist "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" (
    set WRAPPER_JAR="%APP_HOME%\gradle\wrapper\gradle-wrapper.jar"
) else (
    @echo Error: Could not find gradle-wrapper.jar in %APP_HOME%\gradle\wrapper\
    exit /b 1
)

@rem Find java.exe
if defined JAVA_HOME (
    set JAVA_HOME=%JAVA_HOME:"=%
    set JAVA_EXE="%JAVA_HOME%\bin\java.exe"
) else (
    set JAVA_EXE=java.exe
)

%JAVA_EXE% -classpath %WRAPPER_JAR% org.gradle.wrapper.GradleWrapperMain %*
