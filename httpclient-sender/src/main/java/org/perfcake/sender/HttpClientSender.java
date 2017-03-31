/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2016 the original author or authors.
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
import org.perfcake.util.StringTemplate;
import org.perfcake.util.Utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * Sends messages via HTTP protocol.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
public class HttpClientSender extends AbstractSender {

   /**
    * The sender's logger.
    */
   private static final Logger log = LogManager.getLogger(HttpClientSender.class);

   /**
    * HTTP cookies header.
    */
   private static final String COOKIES_HEADER = "Set-Cookie";

   /**
    * The URL where the HTTP request is sent.
    */
   private URL url;

   /**
    * The HTTP method that will be used.
    */
   private Method method = Method.POST;

   /**
    * A string template determining the HTTP method to be used dynamically for each request.
    * If not configured (set to null), static configuration in {@link #method} is used instead.
    */
   private StringTemplate dynamicMethod = null;

   /**
    * HTTP method that should be used for the current send operation, pre-calculated in {@link #preSend(Message, Properties)}.
    */
   private Method currentMethod;

   private HttpClient httpClient = HttpClients.createDefault();

   private HttpUriRequest currentRequest;
   private HttpResponse currentHttpResponse;

   /**
    * Enumeration on available HTTP methods.
    */
   public static enum Method {
      GET, POST, HEAD, OPTIONS, PUT, DELETE, TRACE, PATCH
   }

   /**
    * The list of response codes that are expected to be returned by HTTP response.
    */
   private List<Integer> expectedResponseCodeList = new ArrayList<>();

   /**
    * The property for expected response codes.
    */
   private String expectedResponseCodes = null;

   /**
    * When true, cookies are stored between HTTP requests.
    */
   private boolean storeCookies = false;

   /**
    * Cookies storage for each client.
    */
   private static ThreadLocal<CookieManager> localCookieManager = new ThreadLocal<>();

   /**
    * The request payload.
    */
   private String payload;

   /**
    * The request payload length.
    */
   private int payloadLength;

   @Override
   public void doInit(final Properties messageAttributes) throws PerfCakeException {
      final String targetUrl = safeGetTarget(messageAttributes);
      try {
         if (log.isDebugEnabled()) {
            log.debug("Setting target URL to: " + targetUrl);
         }
         url = new URL(targetUrl);
      } catch (MalformedURLException e) {
         throw new PerfCakeException(String.format("Cannot initialize HTTP connection, invalid URL %s: ", targetUrl), e);
      }
   }

   @Override
   public void doClose() {
      // nop
   }

   /**
    * Sets the value of expectedResponseCodes property.
    *
    * @param expectedResponseCodes
    *    The expectedResponseCodes property to set.
    * @return Instance of this to support fluent API.
    */
   public HttpClientSender setExpectedResponseCodes(final String expectedResponseCodes) {
      this.expectedResponseCodes = expectedResponseCodes;
      setExpectedResponseCodesList(expectedResponseCodes.split(","));
      return this;
   }

   /**
    * Gets the list of expected response codes.
    *
    * @return The list of expected response codes.
    */
   public List<Integer> getExpectedResponseCodeList() {
      return expectedResponseCodeList;
   }

   /**
    * Gets the value of expectedResponseCodes property.
    *
    * @return The expectedResponseCodes.
    */
   public String getExpectedResponseCodes() {
      return expectedResponseCodes;
   }

   /**
    * Sets a list of expected response codes.
    *
    * @param codes
    *    The array of codes.
    * @return Instance of this to support fluent API.
    */
   protected HttpClientSender setExpectedResponseCodesList(final String[] codes) {
      final LinkedList<Integer> numCodes = new LinkedList<Integer>();
      for (final String code : codes) {
         numCodes.add(Integer.parseInt(code.trim()));
      }
      expectedResponseCodeList = numCodes;

      return this;
   }

   /**
    * Checks if the code is expected.
    *
    * @param code
    *    Checked response code.
    * @return true/false according to if the code is expected or not.
    */
   private boolean checkResponseCode(final int code) {
      if (expectedResponseCodeList.isEmpty()) {
         return true;
      }
      for (final int i : expectedResponseCodeList) {
         if (i == code) {
            return true;
         }
      }
      return false;
   }

   @Override
   public void preSend(final Message message, final Properties messageAttributes) throws Exception {
      super.preSend(message, messageAttributes);

      if (storeCookies && localCookieManager.get() == null) {
         localCookieManager.set(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
      }

      currentMethod = getDynamicMethod(messageAttributes);

      payloadLength = 0;
      if (message == null) {
         payload = null;
      } else if (message.getPayload() != null) {
         payload = message.getPayload().toString();
         payloadLength = payload.length();
      }

      final URI uri = url.toURI();
      switch (currentMethod) {
         case GET:
            currentRequest = new HttpGet(uri);
            break;
         case POST:
            currentRequest = new HttpPost(uri);
            break;
         case PUT:
            currentRequest = new HttpPut(uri);
            break;
         case PATCH:
            currentRequest = new HttpPatch(uri);
            break;
         case DELETE:
            currentRequest = new HttpDelete(uri);
            break;
         case HEAD:
            currentRequest = new HttpHead(uri);
            break;
         case TRACE:
            currentRequest = new HttpTrace(uri);
            break;
         case OPTIONS:
            currentRequest = new HttpOptions(uri);
      }

      StringEntity msg = null;
      if (payload != null && (currentRequest instanceof HttpEntityEnclosingRequestBase)) {
         ((HttpEntityEnclosingRequestBase) currentRequest).setEntity(new StringEntity(payload, ContentType.getByMimeType("text/plain; charset=utf-8")));
      }

      if (storeCookies) {
         popCookies();
      }

      if (log.isDebugEnabled()) {
         log.debug("Setting HTTP headers: ");
      }

      // set message properties as HTTP headers
      if (message != null) {
         for (final Entry<Object, Object> property : message.getProperties().entrySet()) {
            final String pKey = property.getKey().toString();
            final String pValue = property.getValue().toString();
            currentRequest.setHeader(pKey, pValue);
            if (log.isDebugEnabled()) {
               log.debug(pKey + ": " + pValue);
            }
         }
      }

      // set message headers as HTTP headers
      if (message != null) {
         if (message.getHeaders().size() > 0) {
            for (final Entry<Object, Object> property : message.getHeaders().entrySet()) {
               final String pKey = property.getKey().toString();
               final String pValue = property.getValue().toString();
               currentRequest.setHeader(pKey, pValue);
               if (log.isDebugEnabled()) {
                  log.debug(pKey + ": " + pValue);
               }
            }
         }
      }

      if (log.isDebugEnabled()) {
         log.debug("End of HTTP headers.");
      }
   }

   @Override
   public Serializable doSend(final Message message, final MeasurementUnit measurementUnit) throws Exception {
      currentHttpResponse = httpClient.execute(currentRequest);

      final int respCode = currentHttpResponse.getStatusLine().getStatusCode();

      if (!checkResponseCode(respCode)) {
         final StringBuilder errorMess = new StringBuilder();
         errorMess.append("The server returned an unexpected HTTP response code: ").append(respCode).append(" ").append("\"").append(currentHttpResponse.getStatusLine().getReasonPhrase()).append("\". Expected HTTP codes are ");
         for (final int code : expectedResponseCodeList) {
            errorMess.append(Integer.toString(code)).append(", ");
         }
         throw new PerfCakeException(errorMess.substring(0, errorMess.length() - 2) + ".");
      }
      return new String(EntityUtils.toByteArray(currentHttpResponse.getEntity()), Utils.getDefaultEncoding());
   }

   @Override
   public void postSend(final Message message) throws Exception {
      super.postSend(message);

      if (storeCookies) {
         pushCookies();
      }
   }

   /**
    * Stores cookies from request connection in the thread local cookie manager.
    */
   private void pushCookies() throws IOException {
      final List<Header> cookiesHeader = Arrays.asList(currentHttpResponse.getHeaders(COOKIES_HEADER));

      if (cookiesHeader != null) {
         for (Header cookie : cookiesHeader) {
            localCookieManager.get().getCookieStore().add(null, HttpCookie.parse(cookie.getValue()).get(0));
         }
      }
   }

   /**
    * Sets the stored cookies to the request connection.
    */
   private void popCookies() throws IOException {
      if (localCookieManager.get().getCookieStore().getCookies().size() > 0) {
         currentRequest.setHeader("Cookie",
            StringUtils.join(localCookieManager.get().getCookieStore().getCookies(), ";"));
      }
   }

   /**
    * Gets the value of HTTP method.
    *
    * @return The HTTP method.
    */
   public Method getMethod() {
      return method;
   }

   /**
    * Sets the value of HTTP method.
    *
    * @param method
    *    The HTTP method to set.
    * @return Instance of this to support fluent API.
    */
   public HttpClientSender setMethod(final Method method) {
      this.method = method;
      return this;
   }

   /**
    * Sets the template used to determine HTTP method dynamically.
    *
    * @param dynamicMethod
    *    The string template to dynamically determine HTTP method.
    * @return Instance of this to support fluent API.
    */
   public HttpClientSender setDynamicMethod(final String dynamicMethod) {
      if (dynamicMethod == null || dynamicMethod.isEmpty()) {
         this.dynamicMethod = null;
      } else {
         this.dynamicMethod = new StringTemplate(dynamicMethod);
      }
      return this;
   }

   /**
    * Gets the template used to determine HTTP method dynamically.
    *
    * @param placeholders
    *    The properties to render the string template.
    * @return The HTTP method.
    */
   public Method getDynamicMethod(final Properties placeholders) {
      if (dynamicMethod == null) {
         return this.method;
      } else {
         return Method.valueOf(dynamicMethod.toString(placeholders));
      }
   }

   /**
    * Gets whether the sender will store cookies between requests.
    *
    * @return If and only if the cookies will be stored between requests.
    */
   public boolean isStoreCookies() {
      return storeCookies;
   }

   /**
    * Sets whether the sender will store cookies between requests.
    *
    * @param storeCookies
    *    True if and only if the cookies should be stored between requests.
    * @return Instance of this to support fluent API.
    */
   public HttpClientSender setStoreCookies(final boolean storeCookies) {
      this.storeCookies = storeCookies;

      return this;
   }
}
