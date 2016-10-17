PerfCake PerfRepo destination plugin
==============================

Description
-----------
The PerfRepo destination allow to store performance result in PerfRepo application [https://github.com/PerfRepo/PerfRepo]

Current plugin version is based on [PerfRepo 1.2](https://github.com/PerfRepo/PerfRepo/tree/1.2).

Installation
------------

1. Install perfrepo-destination plugin into `$PERFCAKE_HOME/lib/plugins` folder.
2. Copy dependent libraries `$PERFREPO_DESTINATION_HOME/target/lib/*.jar` into `$PERFCAKE_HOME/lib/plugins` folder

Properties
----------

- **`metric`** - Defines test metric, is related to reporter. The metric should be assigned to the test in the PerfRepo.
- **`repositoryUrl`** - The URL of running PerfRepo application.
- **`username`** - The username of the PerfRepo user.
- **`password`** - The password of the PerfRepo user.
- **`testUID`** - Defines the test which is executed; it matches the test uid in the PerfRepo.
- **`tags`** - Tags related to test execution. More tags should be separated by ';'.
- **`testExecutionName`** - The name of the Test Execution which is displayed in the PerfRepo.
- **`parameters`** - Parameters of the test executions.
- **`valueParameters`** - Parameters related to the value.
- **`reporterResultName`** - Name of the result in MeasurementUnit results map. If not set, the Measurement#DEFAULT_RESULT is used.
- **`isPercentageRecorded`** - A property that determines if the percentage of a result will be recorded as value parameter. The default value is false.
- **`isIterationRecorded`** - A property that determines if the iteration of a result will be recorded as value parameter. The default value is false.
- **`isTimeRecorded`** - A property that determines if the time of a result will be recorded as value parameter. The default value is false.

Scenario example
----------------

```xml
<?xml version="1.0" encoding="utf-8"?>
<scenario xmlns="urn:perfcake:scenario:7.0">
	<run type="${perfcake.run.type:time}" value="${perfcake.run.duration:300000}"/>
	<generator class="DefaultMessageGenerator" threads="${perfcake.thread.count:100}"/>
	<sender class="DummySender">
		<target>Out There!"</target>
	</sender>
	<reporting>
		<reporter class="ThroughputStatsReporter">
			<destination class="CsvDestination">
				<period type="time" value="30000"/>
				<property name="path" value="${perfcake.scenario}-throughput-stats.csv"/>
			</destination>
			<destination class="PerfRepoDestination">
				<period type="time" value="${perfcake.run.duration:300000}"/>
				<property name="metric" value="throughput"/>
				<property name="repositoryUrl" value="perfrepoapp.org"/>
				<property name="username" value="username"/>
				<property name="password" value="password"/>
				<property name="testUID" value="${perfcake.scenario}"/>
				<property name="tags" value="jdk8;rhel6"/>
				<property name="testExecutionName" value="Performance: ${perfcake.scenario}"/>
				<property name="reporterResultName" value="Average"/>
			</destination>
		</reporter>
	</reporting>
</scenario>
```

