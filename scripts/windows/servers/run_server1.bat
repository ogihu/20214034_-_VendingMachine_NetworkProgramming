@echo off
call "%~dp0..\env.bat"
title SMART VENDING - Server1 (9090)
gradle runServer1 --console=plain
