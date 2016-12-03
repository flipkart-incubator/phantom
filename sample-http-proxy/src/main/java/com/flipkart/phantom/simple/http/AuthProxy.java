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
  private static final String X_PROXY_USER = "X-Proxy-User";
  private static final String X_RESTBUS_USER = "X_RESTBUS_USER";

  public AuthProxy(AuthNConfiguration authNConfiguration, AuthTokenService authTokenService) {
    this.authNConfiguration = authNConfiguration;
    this.authTokenService = authTokenService;
  }

  public HttpResponse doRequest(HttpRequestWrapper httpRequestWrapper) throws Exception {

    String url = "http://" + getPool().getHost()+ ":" + getPool().getPort(); 
        
    String token = authTokenService.fetchToken(url).toAuthorizationHeader();
    List<Map.Entry<String, String>> headers = new ArrayList<>();
    headers.add(new AbstractMap.SimpleEntry<>("Authorization", token));
    headers.add(new AbstractMap.SimpleEntry<>(X_PROXY_USER, authNConfiguration.getProxyUser()));
    headers.add(new AbstractMap.SimpleEntry<>(X_RESTBUS_USER, authNConfiguration.getProxyUser()));
    httpRequestWrapper.setHeaders(headers);

    return super.doRequest(httpRequestWrapper);
  }

}

