PerfCake SeleniumSender plugin
==============================

This sender executes selenium scenarios in form of HTML test suite files. You can generate these HTML files with [Selenium IDE](http://docs.seleniumhq.org/projects/ide/) firefox plugin.

Plugin is based on [selenium server 2.42.2](https://github.com/SeleniumHQ/selenium/tree/selenium-2.42.2/java/server).

Properties used by this sender:

- **`port`** - Specifies the port you wish to run the server on (default is `4444`). If more threads are used, port number will be incremented by `1`.
- **`timeout`** - Specifies the number of seconds that you allow data to wait all in the communications queues before an exception is thrown (default is `1800`).
- **`browser`** - Specifies the browser. You must have installed the selected browser. Some browsers need some extra configurations.
	- Supported values: `*firefoxproxy`, `*firefox`, `*chrome`, `*firefoxchrome`, `*firefox2`, `*firefox3`, `*iexploreproxy`, `*safari`, `*safariproxy`, `*iehta`, `*iexplore`, `*opera`, `*piiexplore`, `*pifirefox`, `*konqueror`, `*mock`, `*googlechrome`, `*webdriver`, `*custom` ([source](https://github.com/SeleniumHQ/selenium/blob/selenium-2.42.2/java/server/src/org/openqa/selenium/server/browserlaunchers/BrowserLauncherFactory.java#L50)).
	- tips:
		- Chromium browser
			- use `*googlechrome`
			- `sudo ln -s /usr/lib/chromium-browser/chromium-browser /usr/bin/google-chrome` (for linux)
		- Another browser
			- use `*custom <path-to-executable-file>`
- **`baseUrl`** - Specifies the base URL.
- **`logFileName`** - Specifies the log file name (default is `selenium-log.html`).

Warning:

- The value of the system property `perfcake.monitoringPeriod` must be greater then the maximum execution time of one iteration of your use case. Otherwise the `InterruptedException` will be thrown.


