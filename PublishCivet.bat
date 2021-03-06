@echo off
E:
del \EclipseJava\Civet\Civet.jar
del \EclipseJava\Civet\Civet.zip
cd \EclipseJava\Civet\bin
rem Copy JPedal classes from storage if we need to package.
jar -cfm ../Civet.jar ..\Civet.MF edu/clemson/lph edu/clemson/lph/civet edu/clemson/lph/controls edu/clemson/lph/db edu/clemson/lph/dialogs edu/clemson/lph/mailman edu/clemson/lph/pdfgen com/cai/webservice
if errorlevel 1 goto jarooops
copy \EclipseJava\Civet\Civet.jar \EclipseJava\Civet\Publish\Civet\Civet.jar
cd \EclipseJava\Civet\Publish
jar -cfM ../Civet.zip Civet
if errorlevel1 goto zipoops
goto done
:zipoops
echo Error in Zip
pause
goto exit
:jaroops
echo Error in Jar
pause
goto exit
:done
cd \EclipseJava\Civet
echo Completed Publishing Civet.jar to \EclipseJava\Civet
pause
:exit
pause
