# Contributing Guide

A short and sweet guide on how to get started contributing to jpizza.

## Getting the source code

If you just want to build jpizza for yourself, you can just download the source from github or clone the main repo.

If you want to contribute any code, Please fork the repo and clone from your fork.

## Compiling JPizza

JPizza uses the gradle build system. So, compiling JPizza is pretty simple! Just navigate your terminal into the folder where you cloned jpizza, then cd into the jpizza folder. From there, run the `gradlew shadowJar` command if you are on macos or linux, or `gradlew.bat shadowJar` if you are on windows.

This will compile jpizza, and the resulting jar will be located at `jpizza/build/libs/jpizza-all.jar`

If you want to create a cross platform release, you can run `gradlew assembleShadowDist` \[or `gradlew.bat assembleShadowDist` if on windows\]

This will create archives which include scripts to run jpizza on unix and windows, alongside with the compiled jpizza jar. These archives can be extracted and put anywhere, and once you add the bin folder to your path, you can run jpizza easily.

## Thats it!

That's really all you need to know to compile jpizza! Have fun!