# ASPUA-Framework
Implementation of an update framework for interactive conflict resolution in ASP-programs as introduced by Thevapalan and Kern-Isberner [^1]

## Overview
The `aspua-framework` provides an API to detect conflicts between different ASP-programs and computes possible solutions for detected conflicts. In its initial implementation, it uses modified update programs as seen in *Towards Interactive Conflict Resolution in ASP Programs*[^1] to detect conflicts. The `aspua-framework` functions as a __proof of concept__ and therefore should not be used as a trustworthy tool for updating ASP-programs. To integrate the `aspua-framework` into a application, please refer to the documentation in the API-class `ASPUAFrameworkAPI`.

## System Requirements & Installation
The following components have to be installed in order to run the `aspua-framework`.

- *Java JDK*: Used Version for Development: [Java SE Development Kit 15.0.1 Windows x64](https://www.oracle.com/java/technologies/javase-jdk15-downloads.html)

- *Apache Maven*: Used Version for Development: [Apache Maven 3.6.3](https://maven.apache.org/download.cgi)

Ensure all components are installed correctly by opening the command line and performing the following steps
1. Test successful *Java JDK* installation by executing <pre><code>java --version</code></pre>
2. Test successful *Maven* installation by executing<pre><code>mvn --version</code></pre>

The listed prompts should display the software's version without any error codes. If errors appear although the software was installed, may check if a correspondant *path variable* to the installation-folder is set in the OS.

## Local execution
The framework should not be seen as a stand-alone application. To test certain implementation details and logs, the framework provides a CLI, which can be executed as followed:

Open the Command Line (OS Terminal or IDE) and navigate to the `aspua-framework` folder. Afterwards, execute <pre><code>mvn clean install</code></pre>

After building the project, perform one of the following steps:
- Run the `CLIViewController` class from your IDE.
- Execute <pre><code>mvn exec:java</code></pre> to invoke the framwork without any arguments. In this case, the files *Example_Thesis_Initial.txt* and *Example_Thesis_Update.txt* of the *./src/main/resources/ASP-Programs-Textfiles* folder are used to show an update process with a conflict.
- Execute <pre><code>mvn exec:java -D"exec.args"="[-f &lt;factoryclassname&gt;] [-p &lt;fileName1&gt; &lt;fileName2&gt;]"</code></pre> to invoke the framwork with arguments
    - `-f <factoryclassname>`: Choose the `IFactoryController`-implementation which is used for the execution. The `factoryclassname` has to specify the classname with its subpackage starting from *de.aspua.framework.Controller*. The initial implementation supports the arguments `CausalRejectionController.CRFileFactory` (default) for textfiles and `CausalRejectionController.CRSerialFactory` for serialized java-objects.
    - `-p <fileName1> <fileName2>`: Choose the programs which will be used for the update process. `fileName1` determines the older program which is updated by the program `fileName2`. The source folder depends on the chosen factory-class. If no filenames are given, the files *Example_Thesis_Initial.txt* and *Example_Thesis_Update.txt* will be used by default.

For further details regarding the the usage of factory-classes and integrating the framework in other applications, please refer to the java-docs of the `de.aspua.framework.Controller.ASPUAFrameworkAPI`-class and other controller-classes of interest.

[^1]: Thevapalan and Kern-Isberner, [Towards Interactive Conflict Resolution in ASP Programs](https://nmr2020.dc.uba.ar/WorkshopNotes.pdf)