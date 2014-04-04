ECE419 Lab 4
Kevin Justin Gumba 	997585117
Tahia Khan		    998897216
Submitted: 	    	April 3, 2014 (Early Bonus)
**Design document also submitted to Professor Amza and Mike Wang.

Assumptions
===========

- ZooKeeper is always running
- Clients will only submit jobs with alphanumeric input
- Clients will not crash

Usage
=====
- Run following commands from /lab4
- Please read instructions on shell file command lines (ex. input hostname:port or input hostname only)

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


Thank you!