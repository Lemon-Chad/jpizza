# Contributing Guide

A short and sweet guide on how to get started contributing to JPizza.

## Getting the source code

If you just want to build JPizza for yourself, you can just download the source from github or clone the main repo.

If you want to contribute any code, Please fork the repo and clone from your fork.

## Compiling JPizza

JPizza uses the gradle build system. So, compiling JPizza is pretty simple! Just navigate your terminal into the folder where you cloned JPizza, then `cd` into the JPizza folder. From there, run the `gradlew shadowJar` command if you are on macos or linux, or `gradlew.bat shadowJar` if you are on windows.

This will compile JPizza, and the resulting jar will be located at `jpizza/build/libs/jpizza-all.jar`

If you want to create a cross platform release, you can run `gradlew assembleShadowDist` \[or `gradlew.bat assembleShadowDist` if on windows\]

This will create archives which include scripts to run JPizza on unix and windows, alongside with the compiled JPizza jar. These archives can be extracted and put anywhere, and once you add the bin folder to your path, you can run JPizza easily.

## Thats it!

That's really all you need to know to compile JPizza! Have fun!
