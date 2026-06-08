@echo off
call "%~dp0..\env.bat"
title SMART VENDING - EndDev
gradle runEndDev --console=plain
