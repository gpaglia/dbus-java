package org.freedesktop.dbus.connections.sasl;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum AuthScheme {
  AUTH_NONE(0, "NONE"),
  AUTH_EXTERNAL(1, "EXTERNAL"),
  AUTH_SHA(2, "DBUS_COOKIE_SHA1"),
  AUTH_ANON(4, "ANONYMOUS");

  private final int authValue;
  private final String authCommandName;

  private static final Map<Integer, AuthScheme> map = new HashMap<>();

  static {
    for (AuthScheme as : AuthScheme.values()) {
      map.put(as.authValue, as);
    }
  }

  AuthScheme(final int as, final String an) {
    authValue = as;
    authCommandName = an;
  }

  @SuppressWarnings("unused")
  public int getSchemeValue() { return authValue; }

  public String getCommandName() { return authCommandName; }

  @SuppressWarnings("unused")
  public static AuthScheme valueOf(int authScheme) {
    return map.get(authScheme);
  }

  public static String[] mechanics(EnumSet<AuthScheme> schemes) {
    return schemes
        .stream()
        .filter(sch -> sch != AUTH_NONE)  // filter out AUTH_NONE as it is a pseudo-scheme
        .map(AuthScheme::getCommandName)
        .toArray(String[]::new);
  }

}
