package org.swellrt.beta.model;

import org.swellrt.beta.model.remote.SNodeRemoteContainer;
import org.waveprotocol.wave.client.common.util.JsoView;

import com.google.gwt.core.client.JavaScriptObject;

public class SUtils {

  public static SNode castToSNode(Object object) throws IllegalCastException {
    
    if (object == null)
      throw new IllegalCastException("Error casting a null object");
    
    if (object instanceof String) {
      return new SPrimitive((String) object);
    } else if (object instanceof Integer) {
      return new SPrimitive((Integer) object);      
    } else if (object instanceof Double) {
      return new SPrimitive((Double) object); 
    } else if (object instanceof Boolean) {
      return new SPrimitive((Boolean) object); 
    } else if (object instanceof SNode) {
      return (SNode) object;
    } else if (object instanceof JavaScriptObject) {
      return new SPrimitive((JavaScriptObject) object);
    }
  
    throw new IllegalCastException("Error casting to primitive SNode");
  }
  
  /**
   * Introspect a generic object (java or javascript)
   * looking up a SNodeRemoteContainer.
   * <p>
   * 
   * @param object
   * @return
   */
  public static SNodeRemoteContainer asContainer(Object object) {
    if (object == null)
      return null;
        
    SNodeRemoteContainer node = null;
    
    if (object instanceof JavaScriptObject) {      
      JsoView jso = JsoView.as((JavaScriptObject) object);
      Object targetObject = jso.getObject("__target__");      
      if (targetObject != null) {
        if (targetObject instanceof SNodeRemoteContainer)
          node = (SNodeRemoteContainer) targetObject;
      }      
    } else if (object instanceof SNodeRemoteContainer) {
      node = (SNodeRemoteContainer) object;
    }
    
    return node;
  }


  
}
