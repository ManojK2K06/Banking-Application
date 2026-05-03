@echo off
echo Compiling...

REM Collect all .java files
dir /s /b src\main\java\*.java > sources.txt

REM Compile
javac -cp BankPro.jar -d out_compiled @sources.txt

IF %ERRORLEVEL% NEQ 0 (
    echo COMPILE FAILED
    pause
    exit /b 1
)

echo Packaging...

REM Copy compiled classes into the jar
cd out_compiled
jar uf ..\BankPro.jar .
cd ..

echo Done. Run: java -jar BankPro.jar
pause