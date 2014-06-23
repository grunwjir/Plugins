package org.perfcake.message.sender;

import org.apache.log4j.Logger;

import org.openqa.selenium.server.RemoteControlConfiguration;
import org.openqa.selenium.server.SeleniumServer;
import org.perfcake.PerfCakeException;
import org.perfcake.message.Message;
import org.perfcake.message.sender.AbstractSender;
import org.perfcake.message.sender.selenium.SeleniumServerAdapter;
import org.perfcake.reporting.MeasurementUnit;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This sender executes selenium scenarios.
 *
 * @author Martin Basovnik <martin.basovnik@gmail.com>
 */
public class SeleniumSender extends AbstractSender {

   /**
    * The sender's logger.
    */
   private static final Logger log = Logger.getLogger(SeleniumSender.class);

   private static AtomicInteger portOffset = new AtomicInteger(0);

   public static final String DEFAULT_LOG_FILE_NAME = "selenium-log.html";

   /**
    * Port.
    */
   private int port = RemoteControlConfiguration.DEFAULT_PORT;

   /**
    * Timeout in seconds.
    */
   private int timeoutInSeconds = RemoteControlConfiguration.DEFAULT_TIMEOUT_IN_SECONDS;

   /**
    * Browser type.
    * 
    * @see <a href="https://github.com/SeleniumHQ/selenium/blob/selenium-2.42.2/java/server/src/org/openqa/selenium/server/browserlaunchers/BrowserLauncherFactory.java#L5">See possible values</a>
    */
   private String browser;

   /**
    * Base url.
    */
   private String baseUrl;

   /**
    * Path to HTML test suite.
    */
   private String testSuitePath;

   /**
    * Selenium log file name.
    */
   private String logFileName = DEFAULT_LOG_FILE_NAME;

   /**
    * Selenium proxy.
    */
   private SeleniumServerAdapter adapter;

   public int getPort() {
      return port;
   }

   public void setPort(int port) {
      this.port = port;
   }

   public int getTimeoutInSeconds() {
      return timeoutInSeconds;
   }

   public void setTimeoutInSeconds(int timeoutInSeconds) {
      this.timeoutInSeconds = timeoutInSeconds;
   }

   public String getBrowser() {
      return browser;
   }

   public void setBrowser(String browser) {
      this.browser = browser;
   }

   public String getBaseUrl() {
      return baseUrl;
   }

   public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
   }

   public String getLogFileName() {
      return logFileName;
   }

   public void setLogFileName(String logFileName) {
      this.logFileName = logFileName;
   }

   @Override
   public void init() throws Exception {
      RemoteControlConfiguration conf = new RemoteControlConfiguration();
      conf.setHTMLSuite(true);
      conf.setPort(port + portOffset.getAndAdd(1));
      conf.setTimeoutInSeconds(timeoutInSeconds);
      conf.setTrustAllSSLCertificates(true);
      adapter = new SeleniumServerAdapter(new SeleniumServer(false, conf));
      adapter.start();
   }

   @Override
   public void close() throws PerfCakeException {
      adapter.stop();
   }

   @Override
   public void preSend(final Message message, final Map<String, String> properties) throws Exception {
      super.preSend(message, properties);
      testSuitePath = message.getPayload().toString();
      log.info("testSuitePath=" + testSuitePath);
      adapter.setBrowserString(browser);
      adapter.setStartURL(baseUrl);
      adapter.setSuiteFilePath(testSuitePath);
      adapter.setResultFilePath(logFileName);
   }

   @Override
   public Serializable doSend(Message arg0, Map<String, String> arg1, MeasurementUnit arg2) throws Exception {
      return adapter.runHtmlSuite();
   }
}
