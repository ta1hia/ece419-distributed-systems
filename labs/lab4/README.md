ECE419 Lab 4
Kevin Justin Gumba 	997585117
Tahia Khan		    998897216
Submitted: 	    	April 3, 2014 (Early Bonus)

Assumptions
===========

- ZooKeeper is always running
- There are initially two JobTrackers and two FileServers (primary and backup)
- Clients will only submit jobs with alphanumeric input
- Clients will not crash

Usage
=====

Make the components:

    	$ make

Run ZooKeeper Server:

    	$ sh runZookeeper.sh

Run JobTracker:

    	$ sh runJobTracker.sh

Run Client:

    	$ sh runClientDriver.sh

Run FileServer:

    	$ sh runFileServer.sh

Run Worker:

    	$ sh runWorker.sh
