#!/bin/bash
set -e

TARGET=patch

PLUGINS='/Users/alex.rojkov/Applications/JDK Mission Control 9.1.app/Contents/Eclipse/plugins/'

CP=$CP:$PLUGINS/org.eclipse.jface_3.35.100.v20241003-1431.jar
CP=$CP:$PLUGINS/org.eclipse.osgi_3.22.0.v20241030-2121.jar
CP=$CP:$PLUGINS/org.eclipse.swt.cocoa.macosx.aarch64_3.128.0.v20241113-2009.jar
CP=$CP:$PLUGINS/org.eclipse.ui.forms_3.13.400.v20240905-1138.jar
CP=$CP:$PLUGINS/org.openjdk.jmc.common_9.1.0.202502051043.jar
CP=$CP:$PLUGINS/org.openjdk.jmc.flightrecorder_9.1.0.202502051043.jar
CP=$CP:$PLUGINS/org.openjdk.jmc.flightrecorder.ui_9.1.0.202502050944.jar
CP=$CP:$PLUGINS/org.openjdk.jmc.ui_9.1.0.202502050944.jar
CP=$CP:$PLUGINS/org.eclipse.equinox.common_3.19.200.v20241004-0654.jar
CP=$CP:$PLUGINS/org.eclipse.core.runtime_3.32.0.v20241003-0436.jar
CP=$CP:$PLUGINS/org.eclipse.ui.workbench_3.134.0.v20241107-2150.jar
CP=$CP:$PLUGINS/org.openjdk.jmc.flightrecorder.serializers_9.1.0.202502051043.jar
CP=$CP:$PLUGINS/org.openjdk.jmc.ui.common_9.1.0.202502050944.jar
CP=$CP:$PLUGINS/org.eclipse.core.commands_3.12.200.v20240627-1019.jar
CP=$CP:$PLUGINS/org.eclipse.equinox.registry_3.12.200.v20241004-0654.jar
CP=$CP:$PLUGINS/org.openjdk.jmc.flightrecorder.rules_9.1.0.202502051043.jar
CP=$CP:$PLUGINS/org.openjdk.jmc.flightrecorder.rules.jdk_9.1.0.202502051043.jar

# 1
javac -cp "$CP" -d $TARGET core/org.openjdk.jmc.flightrecorder/src/main/java/org/openjdk/jmc/flightrecorder/jdk/JdkTypeIDs.java\
 core/org.openjdk.jmc.flightrecorder/src/main/java/org/openjdk/jmc/flightrecorder/jdk/JdkFilters.java\
 core/org.openjdk.jmc.flightrecorder/src/main/java/org/openjdk/jmc/flightrecorder/parser/synthetic/SyntheticAttributeExtension.java
if [ $? -ne 0 ]; then
  echo "Compilation failed for flightrecorder. Exiting." >&2
  exit 1
fi

jar -v --update --file="$PLUGINS/org.openjdk.jmc.flightrecorder_9.1.0.202502051043.jar" -C patch org/

rm -rf $TARGET/*

#2
javac -cp "$CP" -d $TARGET core/org.openjdk.jmc.flightrecorder.rules.jdk/src/main/java/org/openjdk/jmc/flightrecorder/rules/jdk/latency/MethodProfilingRule.java
if [ $? -ne 0 ]; then
  echo "Compilation failed for rules.jdk. Exiting." >&2
  exit 1
fi

jar -v --update --file="$PLUGINS/org.openjdk.jmc.flightrecorder.rules.jdk_9.1.0.202502051043.jar" -C patch org/

rm -rf $TARGET/*

#3
javac -cp "$CP" -d $TARGET application/org.openjdk.jmc.flightrecorder.ui/src/main/java/org/openjdk/jmc/flightrecorder/ui/common/TypeLabelProvider.java\
 application/org.openjdk.jmc.flightrecorder.ui/src/main/java/org/openjdk/jmc/flightrecorder/ui/pages/MethodProfilingPage.java
if [ $? -ne 0 ]; then
  echo "Compilation failed for flightrecorder.ui. Exiting." >&2
  exit 1
fi

jar -v --update --file="$PLUGINS/org.openjdk.jmc.flightrecorder.ui_9.1.0.202502050944.jar" -C patch org/

rm -rf $TARGET/*
