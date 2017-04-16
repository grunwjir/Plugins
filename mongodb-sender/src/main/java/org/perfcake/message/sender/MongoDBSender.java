/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2017 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package org.perfcake.message.sender;

import org.perfcake.PerfCakeException;
import org.perfcake.message.Message;
import org.perfcake.reporting.MeasurementUnit;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import org.bson.BsonDocument;

import java.io.BufferedReader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.Collections;
import java.util.Properties;

/**
 * This sender takes the message, line by line and evaluates it as commands at the connected MongoDB.
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class MongoDBSender extends AbstractSender {

   /**
    * Client for the MongoDB
    */
   private MongoClient mongoClient;

   /**
    * Name of the database to be used
    */
   private String dbName;

   /**
    * MongoDB database object
    */
   private MongoDatabase db;

   /**
    * Username for MongoDB authentication
    */
   private String dbUsername = null;

   /**
    * Password for MongoDB authentication
    */
   private String dbPassword = null;

   /**
    * Size of the connection pool for MongoDB per one Sender instance
    */
   private int connectionPoolSize = 5;

   @Override
   public void doInit(final Properties messageAttributes) throws PerfCakeException {
      final String t = getTarget(messageAttributes);
      final ServerAddress a;

      if (t.contains(":")) {
         final String[] addr = t.split(":", 2);
         final String host = addr[0];
         final int port = Integer.valueOf(addr[1]);
         a = new ServerAddress(host, port);
      } else {
         a = new ServerAddress(t);
      }

      if (dbUsername != null) {
         final MongoCredential c = MongoCredential.createScramSha1Credential(dbUsername, dbName, dbPassword.toCharArray());
         mongoClient = new MongoClient(a, Collections.singletonList(c), MongoClientOptions.builder().connectionsPerHost(connectionPoolSize).build());
      } else {
         mongoClient = new MongoClient(a, MongoClientOptions.builder().connectionsPerHost(connectionPoolSize).build());
      }

      db = mongoClient.getDatabase(dbName);
   }

   @Override
   public void doClose() throws PerfCakeException {
      mongoClient.close();
   }

   @Override
   public Serializable doSend(final Message message, final MeasurementUnit measurementUnit) throws Exception {
      try (StringReader sr = new StringReader(message.getPayload().toString());
            BufferedReader reader = new BufferedReader(sr)) {
         String line = reader.readLine();
         while (line != null) {
            db.runCommand(BsonDocument.parse(line));
            line = reader.readLine();
         }
      }

      return null;
   }

   public String getDbName() {
      return dbName;
   }

   public void setDbName(String dbName) {
      this.dbName = dbName;
   }

   public String getDbUsername() {
      return dbUsername;
   }

   public void setDbUsername(String dbUsername) {
      this.dbUsername = dbUsername;
   }

   public String getDbPassword() {
      return dbPassword;
   }

   public void setDbPassword(String dbPassword) {
      this.dbPassword = dbPassword;
   }

   public int getConnectionPoolSize() {
      return connectionPoolSize;
   }

   public void setConnectionPoolSize(int connectionPoolSize) {
      this.connectionPoolSize = connectionPoolSize;
   }
}
