package org.swellrt.beta.client.rest.operations.params;

public class CredentialImpl implements Credential {

  protected String id;
  protected String password;
  protected boolean remember;


  public CredentialImpl(String id) {
    super();
    this.id = id;
  }

  public CredentialImpl(String id, String password, boolean remember) {
    super();
    this.id = id;
    this.password = password;
    this.remember = remember;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public boolean getRemember() {
    return remember;
  }
}
