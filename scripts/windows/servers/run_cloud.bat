@echo off
call "%~dp0..\env.bat"
title SMART VENDING - Cloud (9093)
gradle runCloud --console=plain
