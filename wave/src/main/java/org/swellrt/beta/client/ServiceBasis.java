package org.swellrt.beta.client;

import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.client.account.ProfileManager;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(namespace = "swell", name = "ServiceBasis")
public interface ServiceBasis {
 
  /**
   * 
   */
  @JsFunction
  public interface ConnectionHandler {
    void exec(String state, SException e);
  }
  
  @JsProperty
  public ProfileManager getProfilesManager();
    
  public void addConnectionHandler(ConnectionHandler h);
  
  public void removeConnectionHandler(ConnectionHandler h);
  
}