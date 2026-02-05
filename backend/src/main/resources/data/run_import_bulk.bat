@echo off
echo ========================================
echo HydroSpark BULK Excel Data Import
echo For files with 1M+ rows
echo ========================================
echo.

REM Check if Python is installed
python --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Python is not installed or not in PATH
    echo Please install Python 3.8+ from https://www.python.org/downloads/
    pause
    exit /b 1
)

REM Install required packages
echo Installing required Python packages...
pip install openpyxl mysql-connector-python --quiet

echo.
echo Starting BULK import...
echo This may take 10-15 minutes for 1M+ rows
echo.

REM Run the BULK import script
python import_excel_bulk.py

echo.
echo ========================================
echo Import process finished!
echo ========================================
echo.
echo Run verify_import.py to check the results
echo.
pause