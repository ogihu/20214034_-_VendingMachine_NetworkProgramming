@echo off
call "%~dp0..\env.bat"
title SMART VENDING - Client3
gradle runClient3 --console=plain
