package com.flipkart.phantom.simple.http;

import com.flipkart.kloud.authn.AuthTokenService;
import com.flipkart.kloud.authn.PrivateKeyCredential;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

/**
 * Created by adarsh.singh on 01/12/16.
 */
public class AuthTokenServiceUtil {
  
  public static AuthTokenService getAuthTokenService(AuthNConfiguration authNConfiguration)
      throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
    PrivateKeyCredential
        privateKeyCredential =
        PrivateKeyCredential.fromPemFile(authNConfiguration.getClientId(),
                                         authNConfiguration.getPrivateKeyPath());
    return new AuthTokenService(
        authNConfiguration.getAuthUrl(), privateKeyCredential);
  }

}
