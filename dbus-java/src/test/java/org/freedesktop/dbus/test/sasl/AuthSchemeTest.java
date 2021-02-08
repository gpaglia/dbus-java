package org.freedesktop.dbus.test.sasl;

import org.freedesktop.dbus.connections.sasl.AuthScheme;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class AuthSchemeTest {

  @Test
  public void testKnownNames() {
    for (AuthScheme as : AuthScheme.values()) {
      final String name = as.getAuthSchemeName();

      assertThat("getFromName(" + name + ")", AuthScheme.fromSchemeName(name), is(as));
    }
  }

  @Test
  public void testunknownName() {
    final String name = "An unknown name";
    assertThat("getFromName(" + name + ")", AuthScheme.fromSchemeName(name), is(AuthScheme.AUTH_NONE));
  }
}
