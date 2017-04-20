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
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import org.perfcake.reporting.destination.builders.TestExecutionDtoBuilder;
import org.perfcake.reporting.destination.builders.ValueDtoBuilder;
import org.perfcake.reporting.destination.builders.ValuesGroupDtoBuilder;
import org.perfcake.reporting.destination.dto.test_execution.ParameterDto;
import org.perfcake.reporting.destination.dto.test_execution.TestExecutionDto;
import org.perfcake.reporting.destination.dto.test_execution.ValuesGroupDto;
import org.perfcake.reporting.reporter.Reporter;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * The destination that store the {@link Measurement} into PerfRepo application.
 *
 */
public class PerfRepoDestination implements Destination {

   private static final String PERCENTAGE = "Percentage";

   private static final String TIME = "Time";

   private static final String ITERATION = "Iteration";


   private RestClient client;

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
    * The repository URL.
    */
   private String repositoryUrl;

   private String username;

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
   private Set<ParameterDto> parsedParameters;

   /**
    * Parsed {@link #tags}.
    */
   private Set<String> parsedTags;

   @Override
   public void open(final Reporter parentReporter) {
      // parse tags
      if (tags != null && !tags.isEmpty()) {
         parsedTags = parseTags(tags);
      }
      // parse execution parameters
      System.out.println("PARAMETERS");
      System.out.println(parameters);
      if (parameters != null && !parameters.isEmpty()) {
         parsedParameters = parseParameters(parameters);
      }
      // test execution name
      if (testExecutionName == null || testExecutionName.isEmpty()) {
         testExecutionName = testUID;
      }

      client = new RestClient(repositoryUrl);

   }

   @Override
   public void close() {
      // nothing to do
   }

   @Override
   public void report(final Measurement m) throws ReportingException {

      // create test execution if it does not exist
      if (testExecutionId == null) {
         TestExecutionDto testExecution = new TestExecutionDtoBuilder()
                 .test(client.getTestByUid(testUID))
                 .started(new Date())
                 .name(testExecutionName)
                 .tags(parsedTags)
                 .executionParameters(parsedParameters)
                 .build();

         testExecutionId = client.createTestExecution(testExecution);
      }
      client.addExecutionValues(testExecutionId , createValuesGroup(m));
   }

   private Set<String> parseTags(String tagsToParse) {
      Set<String> result = new HashSet<>();
      String[] ts = tagsToParse.split(delimiter);
      for (String t : ts) {
         result.add(t);
      }
      return result;
   }

   private Set<ParameterDto> parseParameters(String paramsToParse) {
      Set<ParameterDto> result = new HashSet<>();
      String[] params = paramsToParse.split(delimiter);
      for (String param : params) {
         if (param.contains("=")) {
            String[] values = param.split("=");
            if (values.length == 2) {
               ParameterDto dto = new ParameterDto();
               dto.setName(values[0]);
               dto.setValue(values[1]);
               result.add(dto);
            }
         }
      }
      return result;
   }

   private ValuesGroupDto createValuesGroup(final Measurement m) throws ReportingException {
      Object result;
      // get result value
      if (reporterResultName != null && !reporterResultName.isEmpty()) {
         result = m.get(reporterResultName);
      } else {
         result = m.get();
      }

      ValueDtoBuilder valueBuilder = new ValueDtoBuilder();

      if (result instanceof Double) {
         valueBuilder.value((Double) result);
      } else if (result instanceof Quantity<?>) {
         valueBuilder.value(((Quantity<?>) result).getNumber().doubleValue());
      } else {
         throw new ReportingException("Unknown result type!");
      }

      // value parameters
      if (isIterationRecorded) {
         valueBuilder.parameter(ITERATION, m.getIteration());
      }
      if (isTimeRecorded) {
         valueBuilder.parameter(TIME, m.getTime());
      }
      if (isPercentageRecorded) {
         valueBuilder.parameter(PERCENTAGE, m.getPercentage());
      }

      ValuesGroupDto valuesGroup = new ValuesGroupDtoBuilder()
              .metric(metric)
              .value(valueBuilder.build()).build();

      return valuesGroup;
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

   public String getUsername() {
      return username;
   }

   public void setUsername(String username) {
      this.username = username;
   }

   public String getPassword() {
      return password;
   }

   public void setPassword(String password) {
      this.password = password;
   }
}