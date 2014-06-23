package org.perfcake.message.sender.selenium;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.browserlaunchers.BrowserLauncher;
import org.openqa.selenium.net.Urls;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.server.BrowserSessionFactory.BrowserSessionInfo;
import org.openqa.selenium.server.FrameGroupCommandQueueSet;
import org.openqa.selenium.server.RemoteControlConfiguration;
import org.openqa.selenium.server.SeleniumCommandTimedOutException;
import org.openqa.selenium.server.SeleniumServer;
import org.openqa.selenium.server.browserlaunchers.BrowserOptions;
import org.openqa.selenium.server.htmlrunner.HTMLLauncher;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Unique Session HTMLLauncher.
 *
 * @author Martin Basovnik <martin.basovnik@gmail.com>
 */
public class UniqueSessionHTMLLauncher extends HTMLLauncher {

   static Logger log = Logger.getLogger(UniqueSessionHTMLLauncher.class.getName());

   /**
    * Selenium server.
    */
   private SeleniumServer remoteControl;

   /**
    * Random object.
    */
   private Random rnd;

   public UniqueSessionHTMLLauncher(SeleniumServer remoteControl) {
      super(remoteControl);
      this.remoteControl = remoteControl;
      this.rnd = new Random();
   }

   /**
    * Launches a single HTML Selenium test suite.
    *
    * @param browser - the browserString ("*firefox", "*iexplore" or an executable path)
    * @param browserURL - the start URL for the browser
    * @param suiteFile - a file containing the HTML suite to run
    * @param outputFile - The file to which we'll output the HTML results
    * @param timeoutInSeconds - the amount of time (in seconds) to wait for the browser to finish
    * @param multiWindow - whether to run the browser in multiWindow or else framed mode
    * @return PASSED or FAIL
    * @throws IOException if we can't write the output file
    */
   public String runHTMLSuite(String browser, String browserURL, File suiteFile, File outputFile,
       long timeoutInSeconds, boolean multiWindow) throws IOException {
     if (browser == null) throw new IllegalArgumentException("browser may not be null");
     if (!suiteFile.exists()) {
       throw new IOException("Can't find HTML Suite file:" + suiteFile.getAbsolutePath());
     }
     if (!suiteFile.canRead()) {
       throw new IOException("Can't read HTML Suite file: " + suiteFile.getAbsolutePath());
     }
     remoteControl.addNewStaticContent(suiteFile.getParentFile());

     // DGF this is a hack, but I can't find a better place to put it
     String urlEncodedSuiteFilename = URLEncoder.encode(suiteFile.getName(), "UTF-8");
     String suiteURL;
     if (browser.startsWith("*chrome") || browser.startsWith("*firefox") ||
         browser.startsWith("*iehta") || browser.startsWith("*iexplore")) {
       suiteURL =
           "http://localhost:" + remoteControl.getConfiguration().getPortDriversShouldContact() +
               "/selenium-server/tests/" + urlEncodedSuiteFilename;
     } else {
       suiteURL =
           Urls.toProtocolHostAndPort(browserURL) + "/selenium-server/tests/" + urlEncodedSuiteFilename;
     }
     return runHTMLSuite(browser, browserURL, suiteURL, outputFile, timeoutInSeconds, multiWindow,
         "info");
   }

   /**
    * Launches a single HTML Selenium test suite.
    *
    * @param browser - the browserString ("*firefox", "*iexplore" or an executable path)
    * @param browserURL - the start URL for the browser
    * @param suiteURL - the relative URL to the HTML suite
    * @param outputFile - The file to which we'll output the HTML results
    * @param timeoutInSeconds - the amount of time (in seconds) to wait for the browser to finish
    * @param multiWindow - TestSuite us executed in multiple windows
    * @return PASS or FAIL
    * @throws IOException If we can't write the output file
    */
   public String runHTMLSuite(String browser, String browserURL, String suiteURL, File outputFile, long timeoutInSeconds, boolean multiWindow) throws IOException {
      return runHTMLSuite(browser, browserURL, suiteURL, outputFile, timeoutInSeconds, multiWindow, "info");
   }

   private String runHTMLSuite(String browser, String browserURL, String suiteURL, File outputFile, long timeoutInSeconds, boolean multiWindow, String defaultLogLevel) throws IOException {
      outputFile.createNewFile();
      if (!outputFile.canWrite()) {
         throw new IOException("Can't write to outputFile: " + outputFile.getAbsolutePath());
      }
      long timeoutInMs = 1000L * timeoutInSeconds;
      if (timeoutInMs < 0) {
         log.warning("Looks like the timeout overflowed, so resetting it to the maximum.");
         timeoutInMs = Long.MAX_VALUE;
      }

      RemoteControlConfiguration configuration = remoteControl.getConfiguration();
      remoteControl.handleHTMLRunnerResults(this);

      String sessionId = Long.toString(((long)(rnd.nextDouble() * 1000000)));
      FrameGroupCommandQueueSet.makeQueueSet(sessionId, configuration.getPortDriversShouldContact(), configuration);

      Capabilities browserOptions = configuration.copySettingsIntoBrowserOptions(new DesiredCapabilities());
      browserOptions = BrowserOptions.setSingleWindow(browserOptions, !multiWindow);

      BrowserLauncher launcher = getBrowserLauncher(browser, sessionId, configuration, browserOptions);
      BrowserSessionInfo sessionInfo = new BrowserSessionInfo(sessionId, browser, browserURL, launcher, null);

      remoteControl.registerBrowserSession(sessionInfo);

      // JB: -- aren't these URLs in the wrong order according to declaration?
      launcher.launchHTMLSuite(suiteURL, browserURL);

      sleepTight(timeoutInMs);

      launcher.close();

      remoteControl.deregisterBrowserSession(sessionInfo);

      if (getResults() == null) {
         throw new SeleniumCommandTimedOutException();
      }

      writeResults(outputFile);

      return getResults().getResult().toUpperCase();
   }
}
