# Remologue
A Syslog message logging network client with a JavaFX graphical user interface (GUI).

![](remologue.svg)

## Build and Run
Java Development Kit (JDK) 1.8 or later is required. Change the required version in the Gradle build script.

### Experimental
The experimental **run** shell script builds, runs and packs the software on Unix like operating systems such as Linux and macOS.

To create the macOS application bundle run ```./run pack```and take the executable bundle from `build/deploy/bundles/Remologue.app`.
It only works for JDK 1.8 and it will not be updated.

### Gradle
Build and run Remologue using [Gradle](https://en.wikipedia.org/wiki/Gradle) on Windows, Linux and macOS by running ```gradle build run```.

## Settings
The software settings can be changed in **.remologue** file in the user's home.
