@echo off
title BankPro - Banking System
echo ========================================
echo   BankPro Banking Management System
echo ========================================
echo.
echo Starting application...
java -jar BankPro.jar
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Java not found or failed to start.
    echo Please install Java 17+ from https://adoptium.net
    pause
)
