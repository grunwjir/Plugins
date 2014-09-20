PerfCake SeleniumSender plugin
==============================

Description
-----------
This sender executes selenium scenarios in form of HTML test suite files. You can generate these HTML files with [Selenium IDE](http://docs.seleniumhq.org/projects/ide/) firefox plugin.

Current plugin version is based on [selenium server 2.43.1](https://github.com/SeleniumHQ/selenium/tree/selenium-2.43.1/java/server).

Installation
------------

1. Download [selenium-server-standalone-2.43.1.jar](http://selenium-release.storage.googleapis.com/2.43/selenium-server-standalone-2.43.1.jar) into `$PERFCAKE_HOME/lib/ext` folder.
2. Install selenium plugin into `$PERFCAKE_HOME/lib/plugins` folder.

Properties
----------

- **`target`** - Specifies the base URL.
- **`port`** - Specifies the port you wish to run the server on (default is `4444`). If more threads are used, port number will be incremented by `1`.
- **`timeout`** - Specifies the number of seconds that you allow data to wait all in the communications queues before an exception is thrown (default is `1800`).
- **`browser`** - Specifies the browser. You must have installed the selected browser. Some browsers need some extra configurations.
   - Supported values: `*firefoxproxy`, `*firefox`, `*chrome`, `*firefoxchrome`, `*firefox2`, `*firefox3`, `*iexploreproxy`, `*safari`, `*safariproxy`, `*iehta`, `*iexplore`, `*opera`, `*piiexplore`, `*pifirefox`, `*konqueror`, `*mock`, `*googlechrome`, `*webdriver`, `*custom` ([source](https://github.com/SeleniumHQ/selenium/blob/selenium-2.43.1/java/server/src/org/openqa/selenium/server/browserlaunchers/BrowserLauncherFactory.java#L50)).
   - tips:
      - Chromium browser
         - use `*googlechrome`
         - `sudo ln -s /usr/lib/chromium-browser/chromium-browser /usr/bin/google-chrome` (for linux)
      - Another browser
         - use `*custom <path-to-executable-file>`
- **`logFileName`** - Specifies the log file name (default is `selenium-log.html`).

Scenario example
----------------
- The value of the generator's property `monitoringPeriod` must be greater then the maximum execution time of one iteration of your use case. Otherwise the `InterruptedException` will be thrown.

```xml
<?xml version="2.0" encoding="utf-8"?>
<scenario xmlns="urn:perfcake:scenario:3.0">
   <generator class="DefaultMessageGenerator" threads="${perfcake.thread.count:1}">
      <run type="${perfcake.run.type:iteration}" value="${perfcake.run.duration:1}"/>
      <property name="monitoringPeriod" value="${perfcake.monitoringPeriod:10000}" />
   </generator>
   <sender class="SeleniumSender">
      <property name="target" value="${htmlSuite.startURL}" />
      <property name="browser" value="${htmlSuite.browserString:*firefox}"/>
  </sender>
   <reporting>
      <reporter class="ResponseTimeStatsReporter">
         <destination class="ConsoleDestination">
            <period type="iteration" value="1" />
         </destination>
      </reporter>
   </reporting>
   <messages>
      <message content="${htmlSuite.suiteFilePath}"/>
   </messages>
</scenario>
```

