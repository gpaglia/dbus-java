package org.freedesktop.dbus.connections;

import org.freedesktop.dbus.messages.ExportedObject;
import org.freedesktop.dbus.utils.LoggingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class FallbackContainer {
  private final Logger LOGGER = LoggerFactory.getLogger(getClass());

  FallbackContainer() {
  }

  private final Map<String[], ExportedObject> fallbacks = new HashMap<>();

  public synchronized void add(String path, ExportedObject eo) {
    LOGGER.debug("Adding fallback on {} of {}", path, eo);
    fallbacks.put(path.split("/"), eo);
  }

  public synchronized void remove(String path) {
    LOGGER.debug("Removing fallback on {}", path);
    fallbacks.remove(path.split("/"));
  }

  public synchronized ExportedObject get(String path) {
    int best = 0;
    int i;
    ExportedObject bestobject = null;
    String[] pathel = path.split("/");
    for (String[] fbpath : fallbacks.keySet()) {
      LOGGER.trace("Trying fallback path {} to match {}", LoggingHelper.arraysDeepString(LOGGER.isTraceEnabled(), fbpath),
          LoggingHelper.arraysDeepString(LOGGER.isTraceEnabled(), pathel));
      for (i = 0; i < pathel.length && i < fbpath.length; i++) {
        if (!pathel[i].equals(fbpath[i])) {
          break;
        }
      }
      if (i > 0 && i == fbpath.length && i > best) {
        bestobject = fallbacks.get(fbpath);
        // Patch GP
        best = i;
      }
      LOGGER.trace("Matches {} bestobject now {}", i, bestobject);
    }

    LOGGER.debug("Found fallback for {} of {}", path, bestobject);
    return bestobject;
  }
}