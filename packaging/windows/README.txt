Medicalytics for Windows
========================

Requirements:
  - Windows 10 or newer
  - No Java, Docker, or database installation needed

Download:
  https://github.com/Medicaliticsss/medicalyticsss/releases/latest

How to run:
  1. Extract this folder anywhere (e.g. Desktop\Medicalytics)
  2. Double-click Medicalytics.cmd
  3. Wait for the login window (first launch may take 1-2 minutes)

Data is stored in:
  %LOCALAPPDATA%\Medicalytics

To stop the app:
  Close the Medicalytics window. The database and API stop automatically.

If startup fails:
  1. Close Medicalytics
  2. Check logs in: %LOCALAPPDATA%\Medicalytics\logs
     - backend.log (API)
     - mariadb-error.log (database)
  3. For database errors, delete folder: %LOCALAPPDATA%\Medicalytics
  4. Download the latest zip and start Medicalytics.cmd again
