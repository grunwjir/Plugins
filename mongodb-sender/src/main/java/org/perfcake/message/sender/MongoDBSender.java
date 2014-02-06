/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2014 the original author or authors.
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

import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import org.apache.log4j.Logger;
import org.perfcake.PerfCakeException;
import org.perfcake.message.Message;
import org.perfcake.reporting.MeasurementUnit;

import java.io.BufferedReader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.Map;

/**
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class MongoDBSender extends AbstractSender {

   private MongoClient mongoClient;
   private String dbName;
   private DB db;

   private String dbUsername = null;
   private String dbPassword = null;

   @Override
   public void init() throws Exception {
      if (target.contains(":")) {
         final String[] addr = target.split(":", 2);
         final String host = addr[0];
         final int port = Integer.valueOf(addr[1]);

         mongoClient = new MongoClient(host, port);
      } else {
         mongoClient = new MongoClient(target);
      }

      db = mongoClient.getDB(dbName);

      if (dbUsername != null) {
         if (!db.authenticate(dbUsername, dbPassword.toCharArray())) {
            throw new PerfCakeException("Cannot authenticate with MongoDB. Inspect the credentials provided in the scenario.");
         }
      }
   }

   @Override
   public void close() throws PerfCakeException {
      mongoClient.close();
   }

   @Override
   public Serializable doSend(Message message, Map<String, String> properties, MeasurementUnit mu) throws Exception {
      try (StringReader sr = new StringReader(message.getPayload().toString());
            BufferedReader reader = new BufferedReader(sr)) {
         String line = reader.readLine();
         while (line != null) {
            db.eval(line);
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
}
