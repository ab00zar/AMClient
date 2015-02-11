/*
 * Copyright (c) 2014 INRIA, INSA Rennes
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package eu.paasage.upperware.adaptationmanager.amclient;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.zeromq.ZMQ;
import org.eclipse.emf.ecore.EObject;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;
import org.apache.commons.io.FilenameUtils;

public class ZeromqClient extends Thread {
	
	private String host, port, repositoryName, resourceName, ZeroMQserver; 
	private final static Logger LOGGER = Logger
		.getLogger(ZeromqClient.class.getName());
		
	public ZeromqClient(){
		Properties properties = AMClient.getProperties();
		host = properties.getProperty("host");
		port = properties.getProperty("port");
		repositoryName = properties.getProperty("repositoryName");
		if (repositoryName == null)
			repositoryName = "repo1";
		resourceName = properties.getProperty("resourceName");
		if (resourceName == null)
			resourceName = "Scalarm";
		ZeroMQserver = properties.getProperty("0MQ.Server");
		LOGGER.log(Level.INFO, "0MQ Client : starting");
		//setDaemon(true);
	}
	
	public void zmqClient() throws IOException{
		
		ZMQ.Context context = ZMQ.context(1);
        //  Socket to talk to server
        ZMQ.Socket requester = context.socket(ZMQ.REQ);
        requester.connect(ZeroMQserver);
        
        String delims = "[ ]+";
        
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String line = "";
        String request = in.readLine();
        while(request != null){
			
			String[] commands = request.split(delims);
			
			if(commands[0].equals("event") || commands[0].equals("terminate") || commands[0].equals("test")){
				requester.send(commands[0].getBytes(), 0);
				byte[] reply = requester.recv(0);
				System.out.println(new String(reply));
			
			} else if(commands[0].equals("store")){
				String fileAddress = null;
				if(commands.length == 1){
					showHelp();
					
				} else if(commands[1].equals("file1")){
					fileAddress = "src/test/resources/Scalarm_full.xmi";
					storeModel(fileAddress);
				
				} else if(commands[1].equals("file2")){
					fileAddress = "src/test/resources/Scalarm_full2.xmi";
					storeModel(fileAddress);
					
				} else if(commands[1].equals("file3")){
					fileAddress = "src/test/resources/Scalarm_full3.xmi";
					storeModel(fileAddress);
					
				} else {
					File f = new File(commands[1]);
					if(f.exists() && !f.isDirectory()) {
						String ext = FilenameUtils.getExtension(commands[1]);
						if(ext.equalsIgnoreCase("xmi")){
							storeModel(commands[1]);
						
						} else {
							System.out.println("Please, submit a .xmi file");
							return;
						}
					} else {
						System.out.println("model file not found!");
						return;
					}
				}

			} else {
				showHelp();
			}
			
			request = in.readLine();
		}
		
        in.close();
        requester.close();
        context.term();
    }
 
    public void run(){
		while(true){
			try {
				this.zmqClient();
			} catch (Exception e){
				LOGGER.log(Level.SEVERE, "0MQ Client faild runnig");
			}
		}
	}
	
	public void storeModel(String fileAddress){
		long startTime = System.currentTimeMillis();
		CDOClient cl = new CDOClient(); //Create the CDOClient
		EObject model;
		model = cl.loadModel(fileAddress);
		cl.storeModel(model, resourceName);
		System.out.println("The model is stored successfully");
		long endTime   = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		System.out.println("Exec time is: " + totalTime);
		cl.closeSession();	
	}
	
	public void showHelp(){
		System.out.println("Invalid Command! Please, read the README file");
	}
}
