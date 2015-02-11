AM (Adaptation Manager) Client 
========

About 
-------- 
AMclient is designed to test the AM and work with it remotely. AMclient will be connected to the AM through 0MQ. It acts like a customer and send commands to it's server (AM). These commands are available:

1) Store: Stores a file or specific sample file in the CDO server. The available sample files (Scalarm_full.xmi, Scalarm_full2.xmi, and Scalarm_full3.xmi) are in /src/test/resources/ directory.

2) Event: Simulates a new event for the AM and causes it to be executed again.

3) Terminate: Terminates the AM.


Configure 
-------- 
The environment variable DEFAULT_AMCLIENT_CONFIG_DIR shoulds points to a directory that contains a configuration properties file named "eu.paasage.upperware.adaptationmanager.amclient.properties." This file allows configuring the host name where CDO server resides (host), port on which CDO server listens (port), the name of the CDO repository (repositoryName), the name of the CDO resource (resourceName) and the IP address of 0MQ server (0MQ.Server). 


Commands
--------
1) store <.xmi file path>
2) store file1
         file2
         file3
3) event
4) terminate


Contact 
-------- 
Aboozar Rajabi (aboozar.rajabi@inria.fr)
