@echo off
call "%~dp0..\env.bat"
title SMART VENDING - Client2
gradle runClient2 --console=plain
