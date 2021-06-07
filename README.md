# ASPUA
Answer set programming update application

## Overview
ASPUA is an application which was developed as part of a bachelor thesis on interactive conflict resolution in ASP-Programs, which was originally introduced by Thevapalan and Kern-Isberner [^1]. A precompiled version of the application is ready for download and can be executed as described in the next chapter. Detailed information for local execution without a precompiled version is described afterwards.

## Execution
An executable JAR-archive is provided in the folder *application*. Therefore, a [Java JDK](https://www.oracle.com/java/technologies/javase-jdk15-downloads.html) is required to start up the application. The *application* folder contains a additional *files*-folder, which includes example files for updating. If the *files*-folder is placed in the same folder as the JAR-archive after the download, the application will use the contained files as initial data. Afterwards, the application creates a new folder for internal data, which is used for further executions.

To test the successful *Java JDK* installation, open a command prompt of your OS and execute `java --version`.The command line should display the software's version without any error codes. If errors appear although the software was installed, may check if a correspondant *path variable* to the installation-folder is set in the OS. If the *Java JDK* was installed successfully, the application can be started by navigating to the folder, in which the JAR-archive is saved and executing 
<pre><code>java -jar aspua.jar</code></pre>

## Developer Infos
The application contains two separate maven-projects
### ASPUA-framwork
The `aspua-framework` is an implementation of the update-framework as described by Thevapalan and Kern-Isberner[^1], and was extended by conflict-resolution strategies that were developed in the course of the bachelor thesis.

### ASPUA-gui
The `aspua-gui` provides a GUI to use the `aspua-framework` via a webbrowser. The GUI was developed with the webframework [Vaadin Flow](https://vaadin.com/flow) and requires the `aspua-framework` in order to function.

Although the different modules can be built independently, it is recommended to build this maven (parent-)project before executing the application. A detailed description of the different modules and its usage can be found in the README-files of the corresponding folders. In particular, the java-classes of the `aspua-framework`-module contain detailed java-docs for using the framework in custom applications (especially the `de.aspua.framework.Controller.ASPUAFrameworkAPI`-class for integrating the framework via the API).

[^1]: Thevapalan and Kern-Isberner, [Towards Interactive Conflict Resolution in ASP Programs](https://nmr2020.dc.uba.ar/WorkshopNotes.pdf)