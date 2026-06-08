@echo off
call "%~dp0..\env.bat"
title SMART VENDING - Server2 (9091)
gradle runServer2 --console=plain
