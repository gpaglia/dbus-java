package org.freedesktop.dbus.connections;

import lombok.extern.slf4j.Slf4j;
import org.freedesktop.dbus.messages.ExportedObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class FallbackContainer {

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
      LOGGER.trace("Trying fallback path {} to match {}", Arrays.deepToString(fbpath), Arrays.deepToString(pathel));
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