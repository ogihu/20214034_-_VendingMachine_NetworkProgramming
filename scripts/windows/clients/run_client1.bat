@echo off
call "%~dp0..\env.bat"
title SMART VENDING - Client1
gradle runClient1 --console=plain
