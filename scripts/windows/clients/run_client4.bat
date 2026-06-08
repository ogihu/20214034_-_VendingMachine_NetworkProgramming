@echo off
call "%~dp0..\env.bat"
title SMART VENDING - Client4
gradle runClient4 --console=plain
