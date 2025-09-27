@echo off
echo Starting Fjord IDE...
cd /d "%~dp0"
mvn -pl fjord-ide javafx:run