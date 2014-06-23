package org.perfcake.message.sender.selenium;

import org.apache.log4j.Logger;

import org.openqa.selenium.server.SeleniumServer;
import org.openqa.selenium.server.htmlrunner.HTMLLauncher;

import java.io.File;

/**
 * Selenium server adapter.
 *
 * @author Martin Basovnik <martin.basovnik@gmail.com>
 */
public class SeleniumServerAdapter {

   /**
    * The adapter's logger.
    */
   private static final Logger log = Logger.getLogger(SeleniumServerAdapter.class);

   private static final String BROWSER_STRING_PROPERTY = "htmlSuite.browserString";
   private static final String START_URL_PROPERTY = "htmlSuite.startURL";
   private static final String SUITE_FILE_PATH_PROPERTY = "htmlSuite.suiteFilePath";
   private static final String RESULT_FILE_PATH_PROPERTY = "htmlSuite.resultFilePath";

   /**
    * Selenium server.
    */
   private SeleniumServer server;

   public SeleniumServerAdapter(SeleniumServer server) throws Exception {
      this.server = server;
   }

   public SeleniumServer getSeleniumServer() {
      return server;
   }

   public String getBrowserString() {
      return System.getProperty(BROWSER_STRING_PROPERTY);
   }

   public void setBrowserString(String browserString) {
      System.setProperty(BROWSER_STRING_PROPERTY, browserString);
   }

   public String getStartURL() {
      return System.getProperty(START_URL_PROPERTY);
   }

   public void setStartURL(String startURL) {
      System.setProperty(START_URL_PROPERTY, startURL);
   }

   public String getSuiteFilePath() {
      return System.getProperty(SUITE_FILE_PATH_PROPERTY);
   }

   public void setSuiteFilePath(String suiteFilePath) {
      System.setProperty(SUITE_FILE_PATH_PROPERTY, suiteFilePath);
   }

   public String getResultFilePath() {
      return System.getProperty(RESULT_FILE_PATH_PROPERTY);
   }

   public void setResultFilePath(String resultFilePath) {
      System.setProperty(RESULT_FILE_PATH_PROPERTY, resultFilePath);
   }

   /**
    * Starts the server.
    * @throws Exception If an error occurred.
    */
   public void start() throws Exception {
      server.start();
   }

   /**
    * Stops the server.
    */
   public void stop() {
      server.stop();
   }

   /**
    * Run test suite.
    * <p>
    * Implementation does not use command {@link System#exit(int)}.
    *
    * @return Returns true if, and only if, selenium scenario passed
    * @throws IllegalStateException If the required selenium task properties were not set properly
    */
   public boolean runHtmlSuite() {
      final String result;
      try {
         String suiteFilePath = getRequiredSystemProperty("htmlSuite.suiteFilePath");
         File suiteFile = new File(suiteFilePath).getCanonicalFile();
         if (!suiteFile.exists()) {
            throw new IllegalStateException("Can't find HTML Suite file:" + suiteFile);
         }
         String fileName = suiteFile.getName();
         if (!(fileName.endsWith(".html") || fileName.endsWith(".htm") || fileName.endsWith(".xhtml"))) {
            throw new IllegalStateException("Suite file must have extension .html or .htm or .xhtml");
         }
         server.addNewStaticContent(suiteFile.getParentFile());
         String startURL = getRequiredSystemProperty("htmlSuite.startURL");
         HTMLLauncher launcher = new UniqueSessionHTMLLauncher(server);
         String resultFilePath = getRequiredSystemProperty("htmlSuite.resultFilePath");
         File resultFile = new File(resultFilePath);
         File resultDir = resultFile.getParentFile();
         if ((resultDir != null) && !resultDir.exists() && !resultDir.mkdirs()) {
            throw new IllegalStateException("can't create directory for result file " + resultFilePath);
         }
         resultFile.createNewFile();

         if (!resultFile.canWrite()) {
            throw new IllegalStateException("can't write to result file " + resultFilePath);
         }

         result = launcher.runHTMLSuite(getRequiredSystemProperty("htmlSuite.browserString"), startURL, suiteFile, resultFile, server.getConfiguration().getTimeoutInSeconds(), (!server.getConfiguration().isSingleWindow()));

         return (!"PASSED".equals(result));
      } catch (Exception e) {
         log.error("HTML suite exception seen:");
         throw new IllegalStateException(e);
      }
   }

   private String getRequiredSystemProperty(String name) {
      String value = System.getProperty(name);
      if (value == null) {
         throw new IllegalStateException("expected property " + name + " to be defined");
      }
      return value;
   }
}
