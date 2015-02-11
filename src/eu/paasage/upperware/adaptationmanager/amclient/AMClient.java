/*
 * Copyright (c) 2014 INRIA, INSA Rennes
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
 
package eu.paasage.upperware.adaptationmanager.amclient;

import java.util.Properties;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.FileInputStream;

public class AMClient{

	private final static Logger LOGGER = Logger
			.getLogger(AMClient.class.getName());
	static Properties properties; 

	public static void main(String[] args){	  
		
		properties = loadProperties(args);
		
		try {
			new ZeromqClient().start();
		} catch (Exception e){
			LOGGER.log(Level.SEVERE, "0MQ Client has failed");
		}
	}
	
	private static final String ENV_CONFIG = "AMCLIENT_CONFIG_DIR";
	private static final String DEFAULT_AMCLIENT_CONFIG_DIR = "amclient";

	private static String retrieveConfigurationDirectoryFullPath() {
		String amclientConfigurationFullPath = System.getenv(ENV_CONFIG);
		if (amclientConfigurationFullPath == null) {
			String home = System.getProperty("user.home");
			Path homePath = Paths.get(home);
			amclientConfigurationFullPath = homePath
					.resolve(DEFAULT_AMCLIENT_CONFIG_DIR).toAbsolutePath()
					.toString();
		}
		return amclientConfigurationFullPath;
	}

	private static String retrievePropertiesFilePath(String propertiesFileName) {
		Path configPath = Paths.get(retrieveConfigurationDirectoryFullPath());
		return configPath.resolve(propertiesFileName).toAbsolutePath()
				.toString();
	}
	
	public static Properties loadProperties(String[] args) {
		String propertyPath = retrievePropertiesFilePath("eu.paasage.upperware.adaptationmanager.amclient.properties");
		Properties fileprops = new Properties();
		try {
			fileprops.load(new FileInputStream(propertyPath));
		} catch (java.io.IOException e) {
			LOGGER.log(Level.SEVERE,
					"Failed to load eu.paasage.upperware.adapter.properties");
		}
		Properties result = new Properties();
		result.putAll(fileprops);
		LOGGER.log(Level.INFO, "Properties:" + result);
		return result;
	}
	
	public static Properties getProperties() {
		return properties;
	}
	
	public static String getRetrievePropertiesFilePath() {
		return retrievePropertiesFilePath("eu.paasage.upperware.adaptationmanager.amclient.properties");
	}
}
