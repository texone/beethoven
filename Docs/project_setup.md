# Build Setup

## Build application

To build the project you need to proceed the following steps

1. Make sure the correct java version is installed [java 11](https://www.oracle.com/technetwork/java/javase/downloads/jdk11-downloads-5066655.html)
2. install [maven](http://maven.apache.org/install.html) 
3. check out the creative computing framework from [github](https://github.com/texone/creativecomputing)
4. build the creative computing framework locally using maven `mvn install`
5. check out the BMW treffpunkt production project from [gitlab](https://gitlab.artcom.de/christianr/bmw-treffpunkt-produktion)
6. switch to branch roll_out
7. in the project locate the file `package.bat`and execute it
8. after executing you will find the build applications for all locations inside the target folder

## Build for development

1. follow the same steps as for build application
2. instead of executing `package.bat` use `mvn install` to load all dependencies and `mvn eclipse` to create an eclipse project

## Video Setup

The application relies on the gstreamer framework for video content. Make sure you install the correct [gstreamer](https://gstreamer.freedesktop.org/download/) version. The last testet version is 1.16.2. When you install gstreamer make sure you use the option complete when installing. Also check that the path variable is correctly setup.

!!! In some cases gstreamer needs to be the first entry in the path variables to work so make sure it is setup accordingly!!!