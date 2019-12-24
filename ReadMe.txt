Need: JDK on both server machine and client one.

If your java version is 1.7 or higher, the program should work. If its lower then use the link below to download newer version. 

Download jdk from https://adoptopenjdk.net or use package manager specific to your system.

Go to the project directory in the terminal.
IMPORTANT! Perform all the following actions from that directory.

Compiling:

    javac ForecastEngine.java ForecastClient.java

Running rmiregistry:

    start rmiregistry

Running server:	

Java ForecastEngine


Running client connected to a localhost:

  	Java ForecastClient

Running client connected to a remote host:


Java ForecastClient SERVER_IP_ADDRESS PORT



Once client application is started you'll see the main window.
