PerfCake MongoDBSender plugin
=============================

This sender reads the message line by line and executes them on the specified MongoDB instance.

Properties used by this sender:
target		Address of the MongoDB server, except for the IP address/domain name, it can contain the port number after a colon
dbName		Name of the DB to connect to
dbUsername	[optional] Username used for DB authentication
dbPassword  [optional] Password used for DB authentication
