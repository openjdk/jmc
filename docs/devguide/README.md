# Developing JDK Mission Control
This document explains how to get started developing JDK Mission Control in Eclipse.

## Getting Eclipse
First of all you should download the latest version of Eclipse. JMC is an RCP application, and thus, the easiest way to make changes to it, is to use the Eclipse IDE.

There are various Eclipse bundles out there. Get (at least) the Eclipse IDE for Eclipse Committers. It adds some useful things, like PDE (the Plugin Development Environment), Git, the Marketplace client and more. You can also use the Eclipse IDE for Java Enterprise Developers.

**You will need an Eclipse 2021-06 or later!**

To get to the screen where you can select another packaging than the standard, click on the [Download Packages](https://www.eclipse.org/downloads/eclipse-packages) link on the Eclipse download page.

Install it, start it and create a new workspace for your JMC work. Creating a new workspace is as easy as picking a new name when starting up your Eclipse in the dialog asking for a directory for the workspace:

![Workspace Selection](images/workspace.png)

## Installing JDKs
If you haven't already, you should now first build JMC using the instructions in the [README.md](../../README.md). 

Next set up your JDKs in your Eclipse. Download and install a JDK 17 distribution, then open _Window | Preferences_ and then select _Java / Installed JREs_. Add your favourite JDK 17 (_Add…_) and then use _Java / Installed JREs / Execution Environments_ to set them as defaults for the JDK 17 execution environments.

Setting installed JREs:

![Set Installed JRE](images/setinstalledjre.png)

Setting execution environments:

![Set Execution Environment](images/setexecutionenvironment.png)

Ensure Eclipse compiler is set to Java 17, go to _Preferences | Java / Compiler_ then for
_Compiler compliance level_ choose `17`.

![Se compiler comliance level](images/setcompilercompliancelevel.png)

Creating the user library:

![Create User Library](images/createuserlibrary.png)

Adding the jars:

![image](images/addingjars.png)

Now we need to check a few things…

### Checkpoint
* Is the Jetty server from the build instructions still up and running? (`mvn p2:site --file releng/third-party/pom.xml; mvn jetty:run --file releng/third-party/pom.xml`)
   ![](images/p2site.png)


If yes, go ahead and open up the most recent target file you can find, available under `releng/platform-definitions/platform-definition-{year}-{month}` (__File | Open File__). You should see something like this:

![Platform Target](images/platformtarget.png)

**Wait for all the dependencies to be properly loaded** (check the progress in the lower right corner), then click the _Set as Active Target Platform_ link in the upper right corner.

Now there is one final preparation for the import – we need to turn off certain Maven settings. Go to the preferences, and select _Maven / Errors/Warnings_. Set _Plugin execution not covered by lifecycle configuration_ to _Ignore_, and also _Out-of-date project configuration_ to _Ignore_.

![Maven Settings](images/mavensettings.png)

Now the preparations are done, and we can start importing the projects. Woho!

## Importing the Projects
First we will have to import the `core/` projects, since they are built separately from the rest. Select _File | Import…_ and select _Maven / Existing Maven Project_.

![Import Maven Project](images/importmaven.png)

Click next, and browse into the `jmc/core` folder. Select all the core projects and import them.

Next select _File | Import…_ and select _Maven / Existing Maven Project_ again, but this time from the root.

![Eclipse Config](images/eclipseconfig.png)

After importing that project, we can now launch / debug JMC from within Eclipse:

![Launch JMC](images/launchjmc.png)

## Configuring Development Settings
If you don’t plan on submitting any changes, then this step is optional. The team use shared settings for formatter and macros. Go to the preferences and then to _Java / Code Style / Formatter_. Then click _Import…_ and select the `configuration/ide/eclipse/formatting/formatting.xml`. You should now have the _Mission Control_ formatting settings active:

![Formatter Settings](images/formattersettings.png)

Optional:

If you have the spotbugs plug-in installed, you should also import the spotbugs excludes (`configuration/spotbugs/spotbugs-exclude.xml`). There is also a common dictionary (`configuration/ide/eclipse/dictionary/dictionary.txt`) and templates (`configuration/ide/eclipse/templates/JMC templates.xml`) which you may find useful.

For dynamic working sets, see http://hirt.se/blog/?p=1149.