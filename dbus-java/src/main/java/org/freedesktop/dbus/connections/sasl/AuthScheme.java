package org.freedesktop.dbus.connections.sasl;

import java.text.Collator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public enum AuthScheme {
  AUTH_NONE(0, "NONE"),
  AUTH_EXTERNAL(1, "EXTERNAL"),
  AUTH_SHA(2, "DBUS_COOKIE_SHA1"),
  AUTH_ANON(4, "ANONYMOUS");

  private final int authValue;
  private final String authSchemeName;

  private static final Collator col = Collator.getInstance();

  private static final Map<Integer, AuthScheme> map = new HashMap<>();
  private static final Map<String, AuthScheme> schemeNameMap = new TreeMap<>(col);

  static {
    col.setDecomposition(Collator.FULL_DECOMPOSITION);
    col.setStrength(Collator.PRIMARY);
    for (AuthScheme as : AuthScheme.values()) {
      map.put(as.authValue, as);
      schemeNameMap.put(as.authSchemeName, as);
    }
  }

  AuthScheme(final int as, final String an) {
    authValue = as;
    authSchemeName = an;
  }

  @SuppressWarnings("unused")
  public int getSchemeValue() { return authValue; }

  public String getAuthSchemeName() { return authSchemeName; }

  @SuppressWarnings("unused")
  public static AuthScheme valueOf(int authScheme) { return map.getOrDefault(authScheme, AUTH_NONE); }

  public static AuthScheme fromSchemeName(String sname) { return schemeNameMap.getOrDefault(sname, AUTH_NONE); }

  public static String[] mechanics(EnumSet<AuthScheme> schemes) {
    return schemes
        .stream()
        .filter(sch -> sch != AUTH_NONE)  // filter out AUTH_NONE as it is a pseudo-scheme
        .map(AuthScheme::getAuthSchemeName)
        .toArray(String[]::new);
  }

}
