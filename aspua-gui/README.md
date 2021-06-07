# ASPUA-GUI
GUI for updating ASP-programs with the ASPUA-framework

## Overview

The `aspua-gui` provides a GUI to use the `aspua-framework` via a webbrowser. The GUI was developed with the webframework [Vaadin Flow](https://vaadin.com/flow) and requires the `aspua-framework` in order to function, i.e. a local maven-dependency (i.e. artifact) for the `aspua-framework` module.

## System Requirements & Installation
The following components have to be installed in order to run the `aspua-gui`.

- *Java JDK*: Used Version for Development: [Java SE Development Kit 15.0.1 Windows x64](https://www.oracle.com/java/technologies/javase-jdk15-downloads.html)

- *Apache Maven*: Used Version for Development: [Apache Maven 3.6.3](https://maven.apache.org/download.cgi)

- *Node.js*: Used Version for Development: [Node.js 14.15.1](https://nodejs.org/en/download/)

- *Browser* (To display GUI): Used Browser for Development: [Google Chrome](https://www.google.de/intl/de/chrome/) for Windows 64-Bit (Build Version 86.0.4240.198)
    - Other common browsers like [Mozilla FireFox](https://www.mozilla.org/de/firefox/new/), [Apple Safari](https://support.apple.com/downloads/safari) or [Microsoft Edge](https://www.microsoft.com/de-de/edge) are equally supported. For further information about compatible Browers please refer to [Vaadin Technologie Support](https://vaadin.com/faq#what-technologies-does-vaadin-support). For different browers, it is possible that the appearance slightly varies and minor bugs appear.

Ensure all components are installed correctly by opening the command line and performing the following steps:
1. Test successful *Java JDK* installation by executing <pre><code>java --version</code></pre>
2. Test successful *Maven* installation by executing<pre><code>mvn --version</code></pre>
3. Test successful *Node.js* installation by execution<pre><code>node --version</code></pre>

The listed prompts should display the software's version without any error codes. If errors appear although the software was installed, may check if a correspondant *path variable* to the installation-folder is set in the OS.

For further information regarding the installation of individual components please refer to the linked download pages or the [Vaadin Tutorial](https://vaadin.com/learn/tutorials/modern-web-apps-with-spring-boot-and-vaadin/setting-up-a-java-development-environment).

## Local execution
1. Open the Command Line (OS Terminal or IDE)
2. Navigate into the `aspua-gui` folder
3. Execute <pre><code>mvn clean install</code></pre>
4. Perform one of the following steps:
    - Run `mvn spring-boot:run` to start the application in development mode (auto-reload for changes in code)
    - Run `mvn spring-boot:run -Pproduction` for starting the application in production mode
    - Run the `Application` class from your IDE

After the application has started, the application can be accessed by the url http://localhost:8080/ in the browser of choice. If the build fails or errors occur while accessing the webapp, try to build the parent-project first. If the parent-project was successfully build, try to run the application again as described in step 4.

## IDE specific details
The following description for the usage of Vaadin in certain IDEs are inherited from the initial README of the vaadin project.
Below are the configuration details to start the project using a `spring-boot:run` command. Both Eclipse and Intellij IDEA are covered.

#### Eclipse
- Right click on a project folder and select `Run As` --> `Maven build..` . After that a configuration window is opened.
- In the window set the value of the **Goals** field to `spring-boot:run` 
- You can optionally select `Skip tests` checkbox
- All the other settings can be left to default

Once configurations are set clicking `Run` will start the application

#### Intellij IDEA
- On the right side of the window, select Maven --> Plugins--> `spring-boot` --> `spring-boot:run` goal
- Optionally, you can disable tests by clicking on a `Skip Tests mode` blue button.

Clicking on the green run button will start the application.