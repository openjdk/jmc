package org.openjdk.jmc.util.listversions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ListVersions {

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.out.println("You need to specify the Eclipse version, e.g. 2023-09!");
			System.exit(2);
		}
		String eclipseVersion = args[0];

		String updateSite = String.format("https://download.eclipse.org/releases/%s/", eclipseVersion);
		String compositeArtifactsUrl = updateSite + "compositeArtifacts.jar";

		try (InputStream compositeStream = new URL(compositeArtifactsUrl).openStream();
				ZipInputStream compositeZipStream = new ZipInputStream(compositeStream)) {
			compositeZipStream.getNextEntry();

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document compositeDoc = dBuilder.parse(compositeZipStream);

			NodeList childrenList = compositeDoc.getElementsByTagName("child");
			String subDirectory = null;

			for (int i = 0; i < childrenList.getLength(); i++) {
				Node childNode = childrenList.item(i);
				if (childNode.getNodeType() == Node.ELEMENT_NODE) {
					Element childElement = (Element) childNode;
					subDirectory = childElement.getAttribute("location");
					if (Character.isDigit(subDirectory.charAt(0))) {
						break;
					}
				}
			}

			if (subDirectory == null) {
				System.out.println("Failed to find subdirectory.");
				return;
			}

			String contentJarUrl = updateSite + subDirectory + "/content.jar";

			try (InputStream contentStream = new URL(contentJarUrl).openStream();
					ZipInputStream contentZipStream = new ZipInputStream(contentStream)) {
				contentZipStream.getNextEntry();

				Document contentDoc = dBuilder.parse(contentZipStream);

				NodeList unitList = contentDoc.getElementsByTagName("unit");
				for (int i = 0; i < unitList.getLength(); i++) {
					Node unitNode = unitList.item(i);
					if (unitNode.getNodeType() == Node.ELEMENT_NODE) {
						Element unitElement = (Element) unitNode;
						String id = unitElement.getAttribute("id");
						String version = unitElement.getAttribute("version");
						switch (id) {
						case "org.eclipse.equinox.executable.feature.group":
						case "org.eclipse.pde.feature.group":
						case "org.eclipse.platform.sdk":
							System.out.println("Found unit: " + id + ", Version: " + version);
							break;
						default:
							// Ignoring other units
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
