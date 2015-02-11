/*
 * Copyright (c) 2014 INRIA, INSA Rennes
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package eu.paasage.upperware.adaptationmanager.amclient;

import org.eclipse.emf.cdo.CDOAdapter;
import org.eclipse.emf.cdo.CDOObject;
import org.eclipse.emf.cdo.CDOObjectReference;
import org.eclipse.emf.cdo.CDOState;
import org.eclipse.emf.cdo.common.CDOCommonSession;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.id.CDOIDUtil;
import org.eclipse.emf.cdo.common.revision.CDOIDAndVersion;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.net4j.CDONet4jSessionConfiguration;
import org.eclipse.emf.cdo.net4j.CDONet4jUtil;
import org.eclipse.emf.cdo.session.CDOSession;
import org.eclipse.emf.cdo.session.CDOSessionInvalidationEvent;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.transaction.CDOTransactionFinishedEvent;
import org.eclipse.emf.cdo.view.CDOAdapterPolicy;
import org.eclipse.emf.cdo.view.CDOObjectHandler;
import org.eclipse.emf.cdo.view.CDOQuery;
import org.eclipse.emf.cdo.view.CDOView;
import org.eclipse.emf.cdo.view.CDOViewAdaptersNotifiedEvent;
import org.eclipse.emf.cdo.view.CDOViewInvalidationEvent;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.net4j.FactoriesProtocolProvider;
import org.eclipse.net4j.Net4jUtil;
import org.eclipse.net4j.buffer.IBufferProvider;
import org.eclipse.net4j.protocol.IProtocolProvider;
import org.eclipse.net4j.util.event.IEvent;
import org.eclipse.net4j.util.event.IListener;
import org.eclipse.net4j.util.lifecycle.LifecycleUtil;
import org.eclipse.net4j.util.om.OMPlatform;
import org.eclipse.net4j.util.om.log.PrintLogHandler;
import org.eclipse.net4j.util.om.trace.PrintTraceHandler;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;

import eu.paasage.camel.CamelModel;
import eu.paasage.camel.CamelPackage;
import eu.paasage.camel.RequirementGroup;
import eu.paasage.camel.VMType;
import eu.paasage.camel.deployment.DeploymentModel;
import eu.paasage.camel.deployment.DeploymentPackage;

import eu.paasage.camel.execution.ExecutionModel;
import eu.paasage.camel.execution.ExecutionPackage;
import eu.paasage.camel.organisation.CloudProvider;
import eu.paasage.camel.organisation.DataCenter;
import eu.paasage.camel.organisation.ExternalIdentifier;
import eu.paasage.camel.organisation.Location;

import eu.paasage.camel.organisation.OrganisationFactory;
import eu.paasage.camel.organisation.OrganisationModel;
import eu.paasage.camel.organisation.OrganisationPackage;
import eu.paasage.camel.organisation.ResourceGroup;
import eu.paasage.camel.organisation.Role;
import eu.paasage.camel.organisation.RoleAssignment;
import eu.paasage.camel.organisation.User;
import eu.paasage.camel.organisation.UserGroup;
import eu.paasage.camel.provider.Implies;
import eu.paasage.camel.provider.ProviderModel;
import eu.paasage.camel.provider.ProviderPackage;
import eu.paasage.camel.scalability.ScalabilityModel;
import eu.paasage.camel.scalability.ScalabilityPackage;
import eu.paasage.camel.security.SecurityModel;
import eu.paasage.camel.security.SecurityPackage;
import eu.paasage.camel.sla.AgreementType;
import eu.paasage.camel.sla.SlaPackage;
import eu.paasage.camel.type.TypePackage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.EnumSet;
import java.lang.*;
import java.util.*; 
/**
 * @author Eike Stepper
 */
public class CDOClient
{	
	//A TCP Connector to the CDOServer
	private org.eclipse.net4j.internal.tcp.TCPClientConnector connector;
	//private org.eclipse.net4j.internal.jvm.JVMClientConnector connector;
	//The CDOSession that is created by the CDOClient which will be used to create CDO transactions or views
	private CDOSession session;
	//Configuration object for the CDO session
	private CDONet4jSessionConfiguration configuration = null;
	//Parameters representing the required connection information in order to connect to the CDOServer
	private String host, port, repositoryName;
	private boolean logging = false;
	
	public class MyListener implements IListener{

		public void notifyEvent(IEvent arg0) {
			// TODO Auto-generated method stub
			System.out.println("EVENT: " + arg0);
			if (arg0 instanceof CDOSessionInvalidationEvent){
				CDOSessionInvalidationEvent e = (CDOSessionInvalidationEvent)arg0;
				List<CDOIDAndVersion> newObjs = e.getNewObjects();
				for (CDOIDAndVersion id: newObjs){
					System.out.println("Got new object with id: " + id.getID());
				}
		    }
		}
		
	}
	
	public class MyHandler implements CDOObjectHandler{

		public void objectStateChanged(CDOView arg0, CDOObject arg1,
				CDOState arg2, CDOState arg3) {
			// TODO Auto-generated method stub
			System.out.println("FINALLY NOTIFIED ABOUT OBJECT STATE CHANGE FOR OBJECT: " + arg1 + " WITH STATE: " + arg3);
		}
		
	}
	
	public class MyInformer implements CDOAdapter{
		
		public MyInformer(){
			
		}

		public Notifier getTarget() {
			// TODO Auto-generated method stub
			return null;
		}

		public boolean isAdapterForType(Object arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		public void notifyChanged(Notification arg0) {
			// TODO Auto-generated method stub
			System.out.println("Got Notification: " + arg0);
			System.out.println("Event type: " + arg0.getEventType());
			System.out.println("For object: " + arg0.getFeature());
		}

		public void setTarget(Notifier arg0) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
    private static final String propertyFilePath;
    
    static {
    	propertyFilePath = AMClient.getRetrievePropertiesFilePath();
    	XMIResToResFact();
    }
	
    /*Default constructor for the client which initiates a CDO session*/
	public CDOClient(){
		initSession();
	}
	
    /* This method is required for loading/exporting XMI resources*/
    private static void XMIResToResFact(){
    	Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap( ).put
		("*", 
		new XMIResourceFactoryImpl()
		{
		public Resource createResource(URI uri)
		{
		XMIResource xmiResource = new XMIResourceImpl(uri);
		return xmiResource;
		}
		});
    }	

	/* This method is called in order to get the connection information
	 * that will be used in order to be able to connect correctly to the
	 * CDO Server and create the respective CDOSession
	 */
	private void getConnectionInformation(){
		Properties properties = AMClient.getProperties();
		host = properties.getProperty("host");
		port = properties.getProperty("port");
		repositoryName = properties.getProperty("repository");
		if (repositoryName == null) repositoryName = "repo1";
		String log = properties.getProperty("logging");
		if (log == null || log.equals("off")) logging = false;
		else if (log.equals("on")) logging = true;
		System.out.println("Got host: " + host + " port: " + port + " repository:" + repositoryName);
	}
	
	/*This method is used for initiating a CDO Session starting by obtaining
	connection information from a property file*/
	private void initSession(){
		getConnectionInformation();

		OMPlatform.INSTANCE.setDebugging(logging);
	    OMPlatform.INSTANCE.addLogHandler(PrintLogHandler.CONSOLE);
	    OMPlatform.INSTANCE.addTraceHandler(PrintTraceHandler.CONSOLE);

	    // Prepare receiveExecutor
	    final ThreadGroup threadGroup = new ThreadGroup("net4j"); //$NON-NLS-1$
	    ExecutorService receiveExecutor = Executors.newCachedThreadPool(new ThreadFactory()
	    {
	      public Thread newThread(Runnable r)
	      {
	        Thread thread = new Thread(threadGroup, r);
	        thread.setDaemon(true);
	        return thread;
	      }
	    });
	    
	    //IManagedContainer container = ContainerUtil.createContainer(); 
	    /*IManagedContainer container = IPluginContainer.INSTANCE;
		Net4jUtil.prepareContainer(container);
		TCPUtil.prepareContainer(container);
		//SSLUtil.prepareContainer(container);
		//LifecycleUtil.activate(container);
		CDONet4jUtil.prepareContainer(container); // Register CDO factories*/
		//container.activate();

	    // Prepare bufferProvider
	    IBufferProvider bufferProvider = Net4jUtil.createBufferPool();
	    LifecycleUtil.activate(bufferProvider);

	    IProtocolProvider protocolProvider = new FactoriesProtocolProvider(
	        new org.eclipse.emf.cdo.internal.net4j.protocol.CDOClientProtocolFactory());

	    // Prepare selector
	    org.eclipse.net4j.internal.tcp.TCPSelector selector = new org.eclipse.net4j.internal.tcp.TCPSelector();
	    //org.eclipse.net4j.internal.jvm.JVMSelector selector = new org.eclipse.net4j.internal.jvm.JVMSelector();
	    selector.activate();

	    // Prepare connector
	    connector = new org.eclipse.net4j.internal.tcp.TCPClientConnector();
	    connector.getConfig().setBufferProvider(bufferProvider);
	    connector.getConfig().setReceiveExecutor(receiveExecutor);
	    connector.getConfig().setProtocolProvider(protocolProvider);
	    connector.getConfig().setNegotiator(null);
	    connector.setSelector(selector);
	    connector.setHost(host); //$NON-NLS-1$
	    connector.setPort(Integer.parseInt(port.trim()));
	    connector.activate();
	    
	    //ITCPConnector connector = TCPUtil.getConnector(container, "0.0.0.0:2036");
	    //IConnector connector = SSLUtil.getConnector(container, "0.0.0.0:2036");
	    //JVMUtil.getAcceptor(container, "default");
	    //connector.activate();

	    // Create configuration
	    CDONet4jSessionConfiguration configuration = CDONet4jUtil.createNet4jSessionConfiguration();
	    configuration.setConnector(connector);
	    configuration.setRepositoryName(repositoryName); //$NON-NLS-1$

	    // Open session
	    session = configuration.openNet4jSession();
	    registerCamelPackages();
	    /*session.getPackageRegistry().putEPackage(CamelPackage.eINSTANCE);
	    session.getPackageRegistry().putEPackage(OrganisationPackage.eINSTANCE);
	    session.getPackageRegistry().putEPackage(EcorePackage.eINSTANCE);*/
	}
	
	/* This method is used to register all Packages of Camel meta-model
	 */
	public void registerCamelPackages(){
		registerPackage(CamelPackage.eINSTANCE);
		registerPackage(ScalabilityPackage.eINSTANCE);
		registerPackage(DeploymentPackage.eINSTANCE);
		registerPackage(OrganisationPackage.eINSTANCE);
		registerPackage(ProviderPackage.eINSTANCE);
		registerPackage(SecurityPackage.eINSTANCE);
		registerPackage(ExecutionPackage.eINSTANCE);
		registerPackage(TypePackage.eINSTANCE);
		registerPackage(SlaPackage.eINSTANCE);
	}
	
	/* This method is used for registering an EPackage mapping to the domain
	 * model that will be exploited for creating or manipulating the respective
	 * domain objects. Input parameter: the EPackage to register
	 */
	public void registerPackage(EPackage pack){
		session.getPackageRegistry().putEPackage(pack);
	}
	
	/* This method can be used to open a CDO transaction and return it to
	 * the developer/user. The developer/user should not forget to close 
	 * the respective cdo transaction in the end.
	 */
	public CDOTransaction openTransaction(){
	    	CDOTransaction trans = session.openTransaction();
	    	System.out.println("Opened transaction!");
	    	return trans;
	}
	
	/* This method can be used to open a CDO view and return it to
	 * the developer/user. The developer/user should not forget to close 
	 * the respective cdo view in the end.
	 */
	public CDOView openView(){
	    	CDOView view = session.openView();
	    	System.out.println("Opened view!");
	    	return view;
	}
	
	/* This method can be used to delete an object provided that its cdoID is given
	 * as a String. 
	 */
	public void deleteObject(CDOID uri){
		try{
			CDOTransaction trans = session.openTransaction();
			//CDOID id = CDOIDUtil.createExternal(uri);
			//System.out.println("ID given: " + uri + " ID produced: " + id);
			CDOObject object = trans.getObject(uri);
			deleteObject(object,trans,true);
		}
		catch(Exception e){
			e.printStackTrace();
		}
			
	}
	
	/* This method can be used to delete an object provided that it has been obtained with the
	 * transaction that is also used as input to this method. First, it obtains all
	 * references to the object and deletes them and then deletes the object from its
	 * container. Please be aware that the last input parameter dictates whether the transaction 
	 * will be committed and closed by this method in the end or not. If not, then the user
	 * should be responsible for setting this parameter as true in the last delete statement
	 * in his/her code or for committing and closing the transaction him/herself.
	 */
	public void deleteObject(CDOObject object, CDOTransaction trans, boolean commitAndClose){
		try{
			//Get all references (non-containment associations) to the object
			List<CDOObjectReference> refs = trans.queryXRefs(object);
			for (CDOObjectReference ref: refs){
				CDOObject source = (CDOObject)ref.getSourceObject();
				CDOObject target = (CDOObject)ref.getTargetObject();
				EStructuralFeature feat = ref.getSourceFeature();
				Object eGet = source.eGet(feat);
				List<?> list = null;
				if(eGet instanceof List<?>){
					list = (List<?>)eGet;
					System.out.println("Prev size: is: " + list.size());
					list.remove(target);
					System.out.println("New size: is: " + list.size());
				}
				else{
					source.eSet(feat, null);
				}
			}
			//Get containment association and delete it
			CDOObject parent = (CDOObject)object.eContainer();
			EStructuralFeature feat = object.eContainmentFeature();
			System.out.println("The feature is: " + feat);
			Object eGet = parent.eGet(feat);
			List<?> list = null;
			if (eGet instanceof List<?>){
				list = (List<?>)eGet;
				System.out.println("Prev size: is: " + list.size());
				list.remove(object);
				System.out.println("New size: is: " + list.size());
			}
			else{
				parent.eSet(feat, null);
			}
			if (commitAndClose){
				trans.commit();
				trans.close();
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/* This method is used for closing a CDO transaction. 
	 */
	public void closeTransaction(CDOTransaction trans){
		trans.close();
	}
	
	/* This method is used for closing a CDO view. 
	 */
	public void closeView(CDOView view){
		view.close();
	}
	
	/* This method is used to store a model into a CDOResource with a particular
	 * name. Do not need to open or close a transaction for this as the
	 * method performs them for you in a transparent manner. The input parameters are: the model to store and the name of the
	 * CDOResource to contain it.
	 */
	public void storeModel(EObject model, String resourceName){
		CDOTransaction trans = openTransaction();
		CDOResource cdo = trans.getOrCreateResource(resourceName);
		EList<EObject> list = cdo.getContents();
		list.add(0, model);
		try{
			  trans.commit();
			  trans.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/* This method is used to save a model into the file system in a specific path given as input
	 * The input parameters are: the model to store and the file path to store it in the file system.
	 */
	public void saveModel(EObject model, String pathName){
		final ResourceSet rs = new ResourceSetImpl();
		rs.getPackageRegistry().put(CamelPackage.eNS_URI, CamelPackage.eINSTANCE);
		Resource res = null;
		File f = new File(pathName);
		EList<EObject> contents = null;
		if (f.exists()){
			res = rs.getResource(URI.createFileURI(pathName), true);
			contents = res.getContents();
			contents.clear();
		}
		else{
			res = rs.createResource(URI.createFileURI(pathName));
			contents = res.getContents();
		}
		System.out.println("Got resource: " + res);
		contents.add(model);
		try{
			res.save(null);
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/* This method is used to create a particular model based on the CERIF
	 * meta-model in order to be able to test the functionality of the 
	 * CDOClient in terms of storing and querying about the objects defined
	 * by this model. 
	 */
/*	public EObject createCerifModel(){
		OrganisationModel cm = OrganisationFactory.eINSTANCE.createOrganisationModel();
		cm.setName("MY ORGANISATION MODEL");
		EList<User> users = cm.getUsers();
		EList<UserGroup> ugroups = cm.getUserGroups();
		EList<Role> roles = cm.getRoles();
		EList<RoleAssignment> assigns = cm.getRoleAssigments();
		EList<eu.paasage.camel.organisation.Resource> rgroups = cm.getResources();
		EList<ExternalIdentifier> ids = cm.getExternalIdentifiers();
		EList<DataCenter> dcs = cm.getDataCentres();
		EList<Location> locs = cm.getLocations();
		
		ExternalIdentifier id1 = OrganisationFactory.eINSTANCE.createExternalIdentifier();
		id1.setName("ID1");
		id1.setIdentifier("ID1");
		ids.add(id1);
		
		ExternalIdentifier id2 = OrganisationFactory.eINSTANCE.createExternalIdentifier();
		id2.setName("ID2");
		id2.setIdentifier("ID2");
		ids.add(id2);
		
		ExternalIdentifier id3 = OrganisationFactory.eINSTANCE.createExternalIdentifier();
		id3.setName("ID3");
		id3.setIdentifier("ID3");
		ids.add(id3);
		
		User user1 = OrganisationFactory.eINSTANCE.createUser();
		user1.setLastName("User");
		user1.setFirstName("User1");
		EList<Organisation> worksFor = user1.getOrganisations();
		EList<ExternalIdentifier> exIDs1 = user1.getExternalIdentifiers();
		exIDs1.add(id1);
		exIDs1.add(id2);
		users.add(user1);
		
		User user2 = OrganisationFactory.eINSTANCE.createUser();
		user2.setFirstName("User2");
		user2.setLastName("User");
		users.add(user2);
		exIDs1 = user2.getExternalIdentifiers();
		//exIDs1.add(id2);
		exIDs1.add(id3);
		
		CloudProvider org1 = OrganisationFactory.eINSTANCE.createCloudProvider();
		org1.setEmail("email2");
		org1.setName("Org2");
		org1.setWww("www2");
		org1.setPublic(true);
		cm.setOrganisation(org1);
		worksFor.add(org1);
		
		UserGroup ug1 = OrganisationFactory.eINSTANCE.createUserGroup();
		ug1.setName("ug1");
		EList<User> members = ug1.getUsers();
		members.add(user1);
		ugroups.add(ug1);
		
		Role r1 = OrganisationFactory.eINSTANCE.createRole();
		r1.setName("role1");
		roles.add(r1);

		Role r2 = OrganisationFactory.eINSTANCE.createRole();
		r2.setName("role2");
		roles.add(r2);
		
		RoleAssignment ra1 = OrganisationFactory.eINSTANCE.createRoleAssignment();
		ra1.setID("MY_ROLE_ASSIGNMENT");
		ra1.setRole(r1);
		ra1.setUser(user1);
		ra1.setOrganisation(org1);
		SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd");
		try{
			ra1.setAssignedOn(ft.parse("1976-12-16"));
			ra1.setStart(ft.parse("1977-12-16"));
			ra1.setEnd(ft.parse("1978-12-16"));
			System.out.println("End date: " + ra1.getEnd());
		}
		catch(Exception e){
			e.printStackTrace();
		}
		assigns.add(ra1);
			
		eu.paasage.camel.organisation.Resource r3 = OrganisationFactory.eINSTANCE.createResource();
		rgroups.add(r3);
		
		ResourceGroup rg2 = OrganisationFactory.eINSTANCE.createResourceGroup();
		rg2.setName("RG2");
		EList<eu.paasage.camel.organisation.Resource> res2 = rg2.getResources();
		res2.add(r3);
		rgroups.add(rg2);
		
		ResourceGroup rg1 = OrganisationFactory.eINSTANCE.createResourceGroup();
		rg1.setName("RG1");
		EList<eu.paasage.camel.organisation.Resource> res = rg1.getResources();
		res.add(rg2);
		rgroups.add(rg1);
		
		Location l1 = OrganisationFactory.eINSTANCE.createLocation();
		l1.setLatitude(80);
		l1.setLongitude(175);
		l1.setCity("City1");
		locs.add(l1);
		
		Location l2 = OrganisationFactory.eINSTANCE.createLocation();
		l2.setLatitude(88);
		l2.setLongitude(120);
		l2.setCity("City1");
		locs.add(l2);
		
		DataCenter dc1 = OrganisationFactory.eINSTANCE.createDataCenter();
		dc1.setName("DC1");
		dc1.setCodeName("DC1");
		dc1.setCloudProvider(org1);
		dc1.setLocation(l1);
		dcs.add(dc1);
		
		DataCenter dc2 = OrganisationFactory.eINSTANCE.createDataCenter();
		dc2.setName("DC2");
		dc2.setCodeName("DC2");
		dc2.setCloudProvider(org1);
		dc2.setLocation(l2);
		dcs.add(dc2);
		
		return cm;
	}
	
	/* This method is used to load a model from a particular xmi resource. The model
	 * can then be stored to the CDO Server/Repository. The method takes as input
	 * the path (as a String) where the XML file resides.   
	 */
	public EObject loadModel(String pathName){
		  final ResourceSet rs = new ResourceSetImpl();
		  rs.getPackageRegistry().put(CamelPackage.eNS_URI, CamelPackage.eINSTANCE);
		  Resource res = rs.getResource(URI.createFileURI(pathName), true);
		  System.out.println("Got resource: " + res);
		  EList<EObject> contents = res.getContents();
		  System.out.println("Contents are: " + contents);
		  
		  return contents.get(0);
	  }
	
	/* This method is used to export a model that has been stored in the CDO Server/Repository.
	 * It takes as input three parameters: (a) the name of the CDOResource, (b) the
	 * Class of the model to be exported and (c) the path of the file to be created as a String. 
	 * We must highlight that if the
	 * model required is not at the root of the CDOResource, we assume that it is 
	 * obtained from the root EObject which maps to a CamelModel and that this CamelModel
	 * does not contain other models that have the same type as the requested model (as
	 * the first model of the respective type is actually obtained). We must also
	 * note that the user is responsible of providing correct input parameters as well
	 * as ensuring that the requested model is indeed stored in the CDOResource whose
	 * name is signified in the input parameters.    
	 */
	public void exportModel(String resourceName, Class c, String filePath){
		  
		  CDOTransaction trans = null;
		  try{
			  FileOutputStream fos = new FileOutputStream(filePath);
			  trans = openTransaction();
			  CDOResource resource = trans.getResource(resourceName);
			  EObject obj = resource.getContents().get(0);
			  final ResourceSet rs = new ResourceSetImpl();
			  rs.getPackageRegistry().put(CamelPackage.eNS_URI, CamelPackage.eINSTANCE);
			  
			  if (c.equals(CamelModel.class)){
				  resource.save(fos, null);
			  }
			  else if (c.equals(DeploymentModel.class)){
				  if (obj instanceof DeploymentModel) resource.save(fos, null);
				  else if (obj instanceof CamelModel){
					  CamelModel cm = (CamelModel)obj;
					  DeploymentModel dm = cm.getDeploymentModels().get(0);
					  Resource res = rs.createResource(URI.createFileURI(filePath));
					  res.getContents().add(dm);
					  res.save(fos,null);
				  }
			  }
			  else if (c.equals(ProviderModel.class)){
				  if (obj instanceof ProviderModel) resource.save(fos, null);
				  else if (obj instanceof CamelModel){
					  CamelModel cm = (CamelModel)obj;
					  ProviderModel dm = cm.getProviderModels().get(0);
					  Resource res = rs.createResource(URI.createFileURI(filePath));
					  res.getContents().add(dm);
					  res.save(fos,null);
				  }
			  }
			  else if (c.equals(OrganisationModel.class)){
				  if (obj instanceof OrganisationModel) resource.save(fos, null);
				  else if (obj instanceof CamelModel){
					  CamelModel cm = (CamelModel)obj;
					  OrganisationModel dm = cm.getOrganisationModels().get(0);
					  Resource res = rs.createResource(URI.createFileURI(filePath));
					  res.getContents().add(dm);
					  res.save(fos,null);
				  }
			  }
			  else if (c.equals(ScalabilityModel.class)){
				  if (obj instanceof ScalabilityModel) resource.save(fos, null);
				  else if (obj instanceof CamelModel){
					  CamelModel cm = (CamelModel)obj;
					  ScalabilityModel dm = cm.getScalabilityModels().get(0);
					  Resource res = rs.createResource(URI.createFileURI(filePath));
					  res.getContents().add(dm);
					  res.save(fos,null);
				  }
			  }
			  else if (c.equals(ExecutionModel.class)){
				  if (obj instanceof ExecutionModel) resource.save(fos, null);
				  else if (obj instanceof CamelModel){
					  CamelModel cm = (CamelModel)obj;
					  ExecutionModel dm = cm.getExecutionModels().get(0);
					  Resource res = rs.createResource(URI.createFileURI(filePath));
					  res.getContents().add(dm);
					  res.save(fos,null);
				  }
			  }
			  else if (c.equals(SecurityModel.class)){
				  if (obj instanceof SecurityModel) resource.save(fos, null);
				  else if (obj instanceof CamelModel){
					  CamelModel cm = (CamelModel)obj;
					  SecurityModel dm = cm.getSecurityModels().get(0);
					  Resource res = rs.createResource(URI.createFileURI(filePath));
					  res.getContents().add(dm);
					  res.save(fos,null);
				  }
			  }
			  else if (c.equals(AgreementType.class)){
				  if (obj instanceof AgreementType) resource.save(fos, null);
				  else if (obj instanceof CamelModel){
					  CamelModel cm = (CamelModel)obj;
					  AgreementType dm = cm.getAgreementModels().get(0);
					  Resource res = rs.createResource(URI.createFileURI(filePath));
					  res.getContents().add(dm);
					  res.save(fos,null);
				  }
			  }
			  else if (c.equals(ProviderModel.class)){
				  if (obj instanceof ProviderModel) resource.save(fos, null);
				  else if (obj instanceof CamelModel){
					  CamelModel cm = (CamelModel)obj;
					  ProviderModel dm = cm.getProviderModels().get(0);
					  Resource res = rs.createResource(URI.createFileURI(filePath));
					  res.getContents().add(dm);
					  res.save(fos,null);
				  }
			  }
			  trans.close();
		  }
		  catch(Exception e){
			  e.printStackTrace();
			  if (trans != null) trans.close();
		  }
	  }

	/* This method is used to export a model or instance of EObject in general into a XMI file.
	 * The model/EObject must have been either created programmatically or obtained via
	 * issuing a query. The method takes as input two parameters: (a) the query results 
	 * as an EObject to be exported, (b) the path of the file to be created.
	 * Please note that this method should be called only when a respective CDO transaction 
	 * has been opened - otherwise an exception will be thrown       
	 */
	public void exportModel(EObject model, String filePath){
		  try{
			  final ResourceSet rs = new ResourceSetImpl();
			  rs.getPackageRegistry().put(CamelPackage.eNS_URI, CamelPackage.eINSTANCE);
			  Resource res = rs.createResource(URI.createFileURI(filePath));
			  res.getContents().add(model);
			  res.save(null);
		  }
		  catch(Exception e){
			  e.printStackTrace();
		  }
	  }
	
	/* This method is used to run a query over the contents stored in the 
	 * CDO Store. You do not have to create a view before running the query
	 * as the view is created before the query transparently by this method 
	 * and closed when the query is finished. The user has the optional
	 * choice to store the first result of the query in a XMI file whose name
	 * is given by him/her. The input parameters for this method are: (a) the 
	 * query dialect (OCL, SQL, HQL), (b) the query String itself and (c) the
	 * name of the XMI file in which the first query result will be stored - it
	 * can be null if the user does not want to export the result.    
	 */
  public List<EObject> runQuery(String dialect, String queryStr, String fileName){
	  List<EObject> results = null;
	  if (fileName == null){
		  CDOView view = openView();
		  CDOQuery query = null;
		  query = view.createQuery(dialect, queryStr);
		  results = query.getResult(EObject.class);
		  view.close();
	  }
	  else{
		  CDOTransaction trans = openTransaction();
		  CDOQuery query = null;
		  query = trans.createQuery(dialect, queryStr);
		  results = query.getResult(EObject.class);
  		  exportModel(results.get(0),fileName);
  		  trans.close();
  	  }
  	  return results;
  }
  
  /* This method is used to close the CDOSession that was opened when creating
   * an object of this class - CDOClient. 
   */
  public void closeSession(){
	  session.close();
	  connector.deactivate();
  }
  
}
