/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2013 the original author or authors.
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
package org.perfcake.reporting.destination;

import org.perfcake.reporting.Measurement;
import org.perfcake.reporting.MeasurementUnit;
import org.perfcake.reporting.Quantity;
import org.perfcake.reporting.ReportingException;

import org.perfrepo.client.PerfRepoClient;
import org.perfrepo.model.TestExecution;
import org.perfrepo.model.Value;
import org.perfrepo.model.builder.TestExecutionBuilder;
import org.perfrepo.model.builder.ValueBuilder;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The destination that store the {@link Measurement} into the PerfRepo application.
 *
 * @author Pavel Drozd
 */
public class PerfRepoDestination extends AbstractDestination {

   private static final String PERCENTAGE = "Percentage";

   private static final String TIME = "Time";

   private static final String ITERATION = "Iteration";

   /**
    * Client for the PerfRepo
    */
   private PerfRepoClient client;

   /**
    * Delimiter used to separate tags, parameters and value parameters.
    */
   private static final String delimiter = ";";

   /**
    * Defines test metric, is related to reporter and should be set in scenario. The metric should be assigned to the test.
    */
   private String metric;

   /**
    * Defines the test which is executed; it matches the test uid in the PerfRepo.
    */
   private String testUID;

   /**
    * Parameters of the test executions. More parameters should be separated by {@value #delimiter}.
    * The parameter format should be following: <code>thread=10</code>.
    * Example of more parameters: <code>thread=10;resultSize=10k</code>.
    */
   private String parameters;

   /**
    * Parameters related to the value.
    * The parameter format should be following: <code>thread=10</code>.
    * The more parameters should be separated by {@value #delimiter}.
    */
   private String valueParameters;

   /**
    * The repository URL.
    */
   private String repositoryUrl;

   /**
    * Authentication header for the rest requests (username and password combined into a string "username:password"
    * and then encoded using the RFC2045-MIME variant of Base64).
    */
   private String authenticationHeader;

   /**
    * Username for REST requests.
    */
   private String username;

   /**
    * Password for REST requests.
    */
   private String password;

   /**
    * Tags related to test execution. More tags should be separated by {@value #delimiter}.
    */
   private String tags;

   /**
    * Test Execution name
    */
   private String testExecutionName;

   /**
    * A property that determines if the percentage of a result will be recorded as value parameter. The default value is false.
    */
   private boolean isPercentageRecorded = false;

   /**
    * A property that determines if the iteration of a result will be recorded as value parameter. The default value is false.
    */
   private boolean isIterationRecorded = false;

   /**
    * A property that determines if the time of a result will be recorded as value parameter. The default value is false.
    */
   private boolean isTimeRecorded = false;

   /**
    * Name of the result in {@link MeasurementUnit} results map. If not set, the {@link Measurement#DEFAULT_RESULT} is used.
    */
   private String reporterResultName;

   /**
    * Test Execution Id, when the reporter stores more values for one test execution.
    */
   private Long testExecutionId;

   /**
    * Parsed {@link #parameters}
    */
   private Map<String, String> parsedParameters;

   /**
    * Parsed {@link #tags}.
    */
   private Set<String> parsedTags;

   /**
    * Parsed {@link #valueParameters}.
    */
   private Map<String, String> parsedValueParameters;

   @Override
   public void open() {
      parsedTags = parseTags(tags);
      if (parameters != null && !parameters.isEmpty()) {
         parsedParameters = parseParameters(parameters);
      }
      if (valueParameters != null && !valueParameters.isEmpty()) {
         parsedValueParameters = parseParameters(valueParameters);
      }
      client = new PerfRepoClient(repositoryUrl, "/", username, password);
      if (testExecutionName == null || testExecutionName.isEmpty()) {
         testExecutionName = testUID;
      }
   }

   @Override
   public void close() {
      // nothing to do
   }

   @Override
   public void report(final Measurement m) throws ReportingException {
      if (testExecutionId != null) {
         TestExecutionBuilder tb = TestExecution.builder().id(testExecutionId).name(testExecutionName);
         createValue(tb.value(), m);
         try {
            client.addValue(tb.build());
         } catch (Exception e) {
            throw new ReportingException("Could report to the PerfRepo: ", e);
         }
      } else {
         TestExecutionBuilder testExecutionBuilder = TestExecution.builder().testUid(testUID)
                                                                  .name(testExecutionName);
         testExecutionBuilder.started(new Date());
         for (String tag : parsedTags) {
            testExecutionBuilder.tag(tag);
         }
         if (parsedParameters != null && !parsedParameters.isEmpty()) {
            for (String param : parsedParameters.keySet()) {
               testExecutionBuilder.parameter(param, parsedParameters.get(param));
            }
         }
         createValue(testExecutionBuilder.value(), m);
         try {
            TestExecution te = testExecutionBuilder.build();
            testExecutionId = client.createTestExecution(te);
         } catch (Exception e) {
            throw new ReportingException("Could not report to the PerfRepo: ", e);
         }
      }
   }

   /**
    * Method used to parse tags. The Tags are in format tag1;tag2 separated by {@value #delimiter}
    *
    * @param tagsToParse
    * @return
    */
   private Set<String> parseTags(String tagsToParse) {
      Set<String> result = new HashSet<String>();
      String[] ts = tagsToParse.split(delimiter);
      for (String t : ts) {
         result.add(t);
      }
      return result;
   }

   /**
    * Method used to parse parameters. The parameters are in format key=value separated by {@value #delimiter}
    *
    * @param paramsToParse
    * @return
    */
   private Map<String, String> parseParameters(String paramsToParse) {
      Map<String, String> result = new HashMap<String, String>();
      String[] params = paramsToParse.split(delimiter);
      for (String param : params) {
         if (param.contains("=")) {
            String[] values = param.split("=");
            if (values.length == 2) {
               result.put(values[0], values[1]);
            }
         }
      }
      return result;
   }

   /**
    * Create {@link Value} according to measurement and destination settings.
    *
    * @param vb
    * @param m
    * @throws ReportingException
    */
   private void createValue(ValueBuilder vb, final Measurement m) throws ReportingException {
      Object result = null;
      if (reporterResultName != null && !reporterResultName.isEmpty()) {
         result = m.get(reporterResultName);
      } else {
         result = m.get();
      }
      if (parsedValueParameters != null && !parsedValueParameters.isEmpty()) {
         for (String key : parsedValueParameters.keySet()) {
            vb.parameter(key, parsedValueParameters.get(key));
         }
      }
      if (isIterationRecorded) {
         vb.parameter(ITERATION, String.valueOf(m.getIteration()));
      }
      if (isTimeRecorded) {
         vb.parameter(TIME, String.valueOf(m.getTime()));
      }
      if (isPercentageRecorded) {
         vb.parameter(PERCENTAGE, String.valueOf(m.getPercentage()));
      }
      if (result instanceof Double) {
         vb.resultValue((Double) result);
      } else if (result instanceof Quantity<?>) {
         vb.resultValue(((Quantity<?>) result).getNumber().doubleValue());
      } else {
         throw new ReportingException("Unknown result type!");
      }
      vb.metricName(metric);
   }

   public String getRepositoryUrl() {
      return repositoryUrl;
   }

   public void setRepositoryUrl(String repositoryUrl) {
      this.repositoryUrl = repositoryUrl;
   }

   public String getTags() {
      return tags;
   }

   public void setTags(String tags) {
      this.tags = tags;
   }

   public String getAuthenticationHeader() {
      return authenticationHeader;
   }

   public void setAuthenticationHeader(String authenticationHeader) {
      this.authenticationHeader = authenticationHeader;
   }

   public String getUsername() {
      return username;
   }

   public void setUsername(final String username) {
      this.username = username;
   }

   public String getPassword() {
      return password;
   }

   public void setPassword(final String password) {
      this.password = password;
   }

   public Long getTestExecutionId() {
      return testExecutionId;
   }

   public void setTestExecutionId(Long testExecutionId) {
      this.testExecutionId = testExecutionId;
   }

   public String getMetric() {
      return metric;
   }

   public void setMetric(String metric) {
      this.metric = metric;
   }

   public String getParameters() {
      return parameters;
   }

   public void setParameters(String parameters) {
      this.parameters = parameters;
   }

   public String getValueParameters() {
      return valueParameters;
   }

   public void setValueParameters(String valueParameters) {
      this.valueParameters = valueParameters;
   }

   public String getTestUID() {
      return testUID;
   }

   public void setTestUID(String testUID) {
      this.testUID = testUID;
   }

   public boolean getIsPercentageRecorded() {
      return isPercentageRecorded;
   }

   public void setIsPercentageRecorded(boolean isPercentageRecorded) {
      this.isPercentageRecorded = isPercentageRecorded;
   }

   public boolean getIsIterationRecorded() {
      return isIterationRecorded;
   }

   public void setIsIterationRecorded(boolean isIterationRecorded) {
      this.isIterationRecorded = isIterationRecorded;
   }

   public boolean getIsTimeRecorded() {
      return isTimeRecorded;
   }

   public void setIsTimeRecorded(boolean isTimeRecorded) {
      this.isTimeRecorded = isTimeRecorded;
   }

   public String getReporterResultName() {
      return reporterResultName;
   }

   public void setReporterResultName(String reporterResultName) {
      this.reporterResultName = reporterResultName;
   }

   public String getTestExecutionName() {
      return testExecutionName;
   }

   public void setTestExecutionName(String testExecutionName) {
      this.testExecutionName = testExecutionName;
   }
}