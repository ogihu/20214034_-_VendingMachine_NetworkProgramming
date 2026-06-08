@echo off
call "%~dp0..\env.bat"
title SMART VENDING - Backup (9092)
gradle runBackup --console=plain
