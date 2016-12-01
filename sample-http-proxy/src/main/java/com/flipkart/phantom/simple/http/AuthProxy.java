package com.flipkart.phantom.simple.http;

import com.flipkart.kloud.authn.AuthTokenService;
import com.flipkart.phantom.http.impl.HttpRequestWrapper;
import com.flipkart.phantom.http.impl.SimpleHttpProxy;

import org.apache.http.HttpResponse;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class AuthProxy extends SimpleHttpProxy {

  private final AuthNConfiguration authNConfiguration;
  private final AuthTokenService authTokenService;

  public static final String URL_SCHEME_HOST_SEPARATOR = "://";
  public static final String URL_HOST_PORT_SEPARATOR = ":";
  private static final String X_PROXY_USER = "X-Proxy-User";
  private static final String X_RESTBUS_USER = "X_RESTBUS_USER";

  public AuthProxy(AuthNConfiguration authNConfiguration, AuthTokenService authTokenService) {
    this.authNConfiguration = authNConfiguration;
    this.authTokenService = authTokenService;
  }

  public HttpResponse doRequest(HttpRequestWrapper httpRequestWrapper) throws Exception {

    String token = authTokenService
        .fetchToken(
            generateURL(URI.create(httpRequestWrapper.getUri()))).toAuthorizationHeader();

    List<Map.Entry<String, String>> headers = new ArrayList<>();
    headers.add(new AbstractMap.SimpleEntry<>("Authorization", token));
    headers.add(new AbstractMap.SimpleEntry<>(X_PROXY_USER, authNConfiguration.getProxyUser()));
    headers.add(new AbstractMap.SimpleEntry<>(X_RESTBUS_USER, authNConfiguration.getProxyUser()));
    httpRequestWrapper.setHeaders(headers);

    return super.doRequest(httpRequestWrapper);
  }

  private String generateURL(URI uri) {
    try {
      return uri.getScheme() + URL_SCHEME_HOST_SEPARATOR + uri.getHost() + URL_HOST_PORT_SEPARATOR
             + (uri.getPort() == -1 ? uri.toURL().getDefaultPort() : uri.getPort());
    } catch (MalformedURLException e) {
      throw new RuntimeException("Invalid url", e);
    }
  }

}

