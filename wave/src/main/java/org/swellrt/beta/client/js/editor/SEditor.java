package org.swellrt.beta.client.js.editor;


import java.util.HashMap;
import java.util.Map;

import org.swellrt.beta.client.ServiceFrontend;
import org.swellrt.beta.client.ServiceFrontend.ConnectionHandler;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.client.common.util.LogicalPanel;
import org.waveprotocol.wave.client.common.util.UserAgent;
import org.waveprotocol.wave.client.doodad.diff.DiffAnnotationHandler;
import org.waveprotocol.wave.client.doodad.diff.DiffDeleteRenderer;
import org.waveprotocol.wave.client.doodad.link.LinkAnnotationHandler;
import org.waveprotocol.wave.client.doodad.link.LinkAnnotationHandler.LinkAttributeAugmenter;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.EditorImpl;
import org.waveprotocol.wave.client.editor.EditorImplWebkitMobile;
import org.waveprotocol.wave.client.editor.EditorSettings;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.Editors;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.misc.StyleAnnotationHandler;
import org.waveprotocol.wave.client.editor.content.paragraph.LineRendering;
import org.waveprotocol.wave.client.editor.keys.KeyBindingRegistry;
import org.waveprotocol.wave.client.widget.popup.PopupChrome;
import org.waveprotocol.wave.client.widget.popup.PopupChromeProvider;
import org.waveprotocol.wave.client.widget.popup.simple.Popup;
import org.waveprotocol.wave.common.logging.AbstractLogger;
import org.waveprotocol.wave.common.logging.AbstractLogger.Level;
import org.waveprotocol.wave.common.logging.LogSink;
import org.waveprotocol.wave.model.conversation.Blips;
import org.waveprotocol.wave.model.document.util.LineContainers;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;

import jsinterop.annotations.JsType;

@JsType(namespace = "swellrt", name = "Editor")
public class SEditor {

  //
  // public flag names
  //
    
  protected static int FLAG_LOG = 1;
  protected static int FLAG_DEBUG_DIALOG = 2;
  protected static int FLAG_UNDO = 3;
  protected static int FLAG_FANCY_CURSOR_BIAS = 4;
  protected static int FLAG_SEMANTIC_COPY_PASTE = 5;
  protected static int FLAG_WHITELIST_EDITOR = 6;
  protected static int FLAG_WEBKIT_COMPOSITION = 7;
  
  protected static final Map<Integer, Boolean> SETTINGS = new HashMap<Integer, Boolean>();
  
  static {
    SETTINGS.put(FLAG_LOG, true);
    SETTINGS.put(FLAG_DEBUG_DIALOG, true);
    SETTINGS.put(FLAG_UNDO, true);
    SETTINGS.put(FLAG_FANCY_CURSOR_BIAS, true);
    SETTINGS.put(FLAG_SEMANTIC_COPY_PASTE, false);
    SETTINGS.put(FLAG_WHITELIST_EDITOR, false);
    SETTINGS.put(FLAG_WEBKIT_COMPOSITION, true);
  }
  
  public static void setFlag(int flag, boolean value) {
    SETTINGS.put(flag, value);
  }
  
  public static boolean getFlag(int flag) {
    return SETTINGS.get(flag);
  }
  
  //
  // Static private properties
  //
  
  private static final String TOPLEVEL_CONTAINER_TAGNAME = "body";
  
  /*
   * A browser's console log
   */
  protected static class ConsoleLogSink extends LogSink {

    private native void console(String msg) /*-{
      console.log(msg);
    }-*/;

    @Override
    public void log(Level level, String message) {
      console("[" + level.name() + "] " + message);
    }

    @Override
    public void lazyLog(Level level, Object... messages) {
      for (Object o : messages) {
        log(level, o.toString());
      }

    }

  }
  
  /**
   *
   */
  protected static class CustomLogger extends AbstractLogger {

    public CustomLogger(LogSink sink) {
      super(sink);
    }

    @Override
    public boolean isModuleEnabled() {
      return getFlag(FLAG_LOG);
    }

    @Override
    protected boolean shouldLog(Level level) {
      return getFlag(FLAG_LOG);
    }
  }

  // 
  // Put together here all static initialization.
  // Delegate all registry instances to those in Editor class,
  // but put here all initialization logic.
  //
  // Use always Editor.ROOT_REGISTRIES as reference for editor's registers
  
  static {
    
    EditorStaticDeps.logger = new CustomLogger(new ConsoleLogSink());
    
    Editors.initRootRegistries();

           
    EditorStaticDeps.setPopupProvider(Popup.LIGHTWEIGHT_POPUP_PROVIDER);
    EditorStaticDeps.setPopupChromeProvider(new PopupChromeProvider() {
      public PopupChrome createPopupChrome() {
        return null;
      }
    });

    //
    // Register Doodads: all are statically handled
    //

    
    // Code taken from RegistriesHolder
    Blips.init();
    LineRendering.registerContainer(TOPLEVEL_CONTAINER_TAGNAME, Editor.ROOT_REGISTRIES.getElementHandlerRegistry());
    LineContainers.setTopLevelContainerTagname(TOPLEVEL_CONTAINER_TAGNAME);

    
    StyleAnnotationHandler.register(Editor.ROOT_REGISTRIES);
    
    // Listen for Diff annotations to paint new content or to insert a
    // delete-content tag
    // to be rendered by the DiffDeleteRendere
    DiffAnnotationHandler.register(
        Editor.ROOT_REGISTRIES.getAnnotationHandlerRegistry(),
        Editor.ROOT_REGISTRIES.getPaintRegistry());
    
    DiffDeleteRenderer.register(
        Editor.ROOT_REGISTRIES.getElementHandlerRegistry());
    
    //
    // Reuse existing link annotation handler, but also support external
    // controller to get notified on mutation or input events
    //
    LinkAnnotationHandler.register(Editor.ROOT_REGISTRIES, new LinkAttributeAugmenter() {
      @Override
      public Map<String, String> augment(Map<String, Object> annotations, boolean isEditing,
          Map<String, String> current) {
        return current;
      }
    });
    
    // TODO register widgets. Widgets definitions are (so far) statically registered
    // so they shouldn't be associated with any particular instance of SEditor
    
    /*
    widgetRegistry.each(new ProcV<JsoWidgetController>() {

      @Override
      public void apply(String key, JsoWidgetController value) {
        value.setEditorJsFacade(editorJsFacade);
      }
      
    });

    WidgetDoodad.register(Editor.ROOT_REGISTRIES.getElementHandlerRegistry(), widgetRegistry);
    */
    
  }
  
  
  public static SEditor createWithId(String containerId) throws SException {
    Element containerElement = DOM.getElementById(containerId);
    if (containerElement == null || !containerElement.getNodeName().equalsIgnoreCase("div"))
      throw new SException(SException.INTERNAL_ERROR, null, "Container element must be a div");
    
    SEditor se = new SEditor(containerElement);  
    return se;
  }
  
  public static SEditor createWithElement(Element containerElement) throws SException {
    if (containerElement == null || !containerElement.getNodeName().equalsIgnoreCase("div"))
      throw new SException(SException.INTERNAL_ERROR, null, "Container element must be a div");
    
    SEditor se = new SEditor(containerElement);  
    return se;
  }
  
  public static SEditor create() {
    SEditor se = new SEditor();  
    return se;
  }
  
  
  private LogicalPanel.Impl editorPanel;
  
  /** Don't use this prop directly, use getter instead */
  private EditorImpl editor;  
  /** Don't use this prop directly, use getter instead */
  private KeyBindingRegistry keyBindingRegistry;
  
  /** A service to listen to connection events */
  ServiceFrontend serviceFrontend;
  
  private boolean wasEditingOnDiscconnect= true;
  
  private ConnectionHandler connectionHandler = new ConnectionHandler() {

    @Override
    public void exec(String state, SException e) {
      if (!state.equals(ServiceFrontend.STATE_CONNECTED)) {
        
        if (editor.isEditing()) {
          wasEditingOnDiscconnect = true;      
          edit(false);
        }

      } else {
        
        if (wasEditingOnDiscconnect && !editor.isEditing())
          edit(true);
        
      }
    }
    
  };
  
  /**
   * Create editor instance tied to a DOM element
   * @param containerElement
   */
  protected SEditor(final Element containerElement) {
    this.editorPanel = new LogicalPanel.Impl() {
      {
        setElement(containerElement);
      }
    };
  }
  
  /**
   * Create editor instance no tied to a DOM element
   */
  protected SEditor() {
    this.editorPanel = new LogicalPanel.Impl() {
      {
        setElement(Document.get().createDivElement());
      }
    };
  }
  
  /**
   * Attach the editor panel to an existing DOM element
   * iff the panel is not already attached.
   * 
   * @param element the parent element
   */
  public void setParent(Element element) {

    if (editorPanel.getParent() != null) {
      editorPanel.getElement().removeFromParent();
    }

    if (element != null) {
      element.appendChild(editorPanel.getElement());
    }

  }
  
  
  public void set(STextWeb text) throws SException {

    Editor e = getEditor();
    // Reuse existing editor.
    if (e.hasDocument()) {
      e.removeContentAndUnrender();
      e.reset();
    }
    
    ContentDocument doc = text.getContentDocument();

    // Ensure the document is rendered and listen for events
    // in a deattached DOM node
    text.setInteractive();

    // Add the document's root DOM node to the editor's panel
    editorPanel.getElement()
        .appendChild(doc.getFullContentView().getDocumentElement().getImplNodelet());

    // make editor aware of the document
    e.setContent(doc);

  }
  
  
  public void edit(boolean editOn) {
    if (editor != null && editor.hasDocument()) {
      if (editor.isEditing() != editOn) 
        editor.setEditing(editOn);
    }
  }
  
  
  public void clean() {
    if (editor != null && editor.hasDocument()) {
      // editor.removeUpdateListener(this);
      editor.removeContentAndUnrender();
      editor.reset();   
    }
  }
  
  public boolean isEditing() {
    return editor != null && editor.isEditing();
  }
  
  public boolean hasDocument() {
    return editor != null && editor.hasDocument();
  }
  
  /**
   * Make editor instance to listen to service's connection
   * events.
   * <p>
   * We prefer to register the service on the editor instead
   * the contrary way, because service must be agnostic
   * from any platform dependent component.
   * 
   * @param serviceFrontend
   */
  public void registerService(ServiceFrontend serviceFrontend) {
    this.serviceFrontend = serviceFrontend;
    this.serviceFrontend.addConnectionHandler(connectionHandler);
  }
  
  public void unregisterService() {
    if (serviceFrontend != null)
      serviceFrontend.removeConnectionHandler(connectionHandler);
  }
  
  protected EditorSettings getSettings() {
    
    return new EditorSettings()
    .setHasDebugDialog(SETTINGS.get(FLAG_DEBUG_DIALOG))
    .setUndoEnabled(SETTINGS.get(FLAG_UNDO))
    .setUseFancyCursorBias(SETTINGS.get(FLAG_FANCY_CURSOR_BIAS))
    .setUseSemanticCopyPaste(SETTINGS.get(FLAG_SEMANTIC_COPY_PASTE))
    .setUseWhitelistInEditor(SETTINGS.get(FLAG_WHITELIST_EDITOR))
    .setUseWebkitCompositionEvents(SETTINGS.get(FLAG_WEBKIT_COMPOSITION));
    
  }
  
  protected KeyBindingRegistry getKeyBindingRegistry() {
    
    if (keyBindingRegistry == null) 
      keyBindingRegistry = new KeyBindingRegistry();
    
    return keyBindingRegistry;
  }
  
  protected Editor getEditor() {
    
    if (editor == null) {
      editor =
        UserAgent.isMobileWebkit() ? new EditorImplWebkitMobile(false, editorPanel.getElement()) 
            : new EditorImpl(false, editorPanel.getElement());
       
        editor.init(null, getKeyBindingRegistry(), getSettings()); 
    }
    return editor;
  }
 
}
