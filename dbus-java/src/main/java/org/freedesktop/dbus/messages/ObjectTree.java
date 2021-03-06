/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson
   Copyright (c) 2017-2019 David M.

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus.messages;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * Keeps track of the exported objects for introspection data
 */
@Slf4j
public class ObjectTree {

  static class TreeNode {
    // CHECKSTYLE:OFF
    String name;
    ExportedObject object;
    String data;
    TreeNode right;
    TreeNode down;
    // CHECKSTYLE:ON

    TreeNode(String _name) {
      this.name = _name;
    }

    @SuppressWarnings("unused")
    TreeNode(String _name, ExportedObject _object, String _data) {
      this.name = _name;
      this.object = _object;
      this.data = _data;
    }
  }

  private final TreeNode root;

  public ObjectTree() {
    root = new TreeNode("");
  }

  public static final Pattern SLASH_PATTERN = Pattern.compile("/");

  private TreeNode recursiveFind(TreeNode _current, String _path) {
    if ("/".equals(_path)) {
      return _current;
    }
    String[] elements = _path.split("/", 2);
    // this is us or a parent node
    if (_path.startsWith(_current.name)) {
      // this is us
      if (_path.equals(_current.name)) {
        return _current;
      }
      // recurse down
      else {
        if (_current.down == null) {
          return null;
        } else {
          return recursiveFind(_current.down, elements[1]);
        }
      }
    } else if (_current.right == null) {
      return null;
    } else if (0 > _current.right.name.compareTo(elements[0])) {
      return null;
    }
    // recurse right
    else {
      return recursiveFind(_current.right, _path);
    }
  }

  @SuppressWarnings("UnusedReturnValue")
  private TreeNode recursiveAdd(TreeNode _current, String _path, ExportedObject _object, String _data) {
    String[] elements = SLASH_PATTERN.split(_path, 2);
    // this is us or a parent node
    if (_path.startsWith(_current.name)) {
      // this is us
      if (1 == elements.length || "".equals(elements[1])) {
        _current.object = _object;
        _current.data = _data;
      }
      // recurse down
      else {
        if (_current.down == null) {
          String[] el = elements[1].split("/", 2);
          _current.down = new TreeNode(el[0]);
        }
        // _current.down = recursiveAdd(_current.down, elements[1], _object, _data);
        recursiveAdd(_current.down, elements[1], _object, _data);
      }
    }
    // need to create a new sub-tree on the end
    else if (_current.right == null) {
      _current.right = new TreeNode(elements[0]);
      // _current.right = recursiveAdd(_current.right, _path, _object, _data);
      recursiveAdd(_current.right, _path, _object, _data);
    }
    // need to insert here
    else if (0 > _current.right.name.compareTo(elements[0])) {
      TreeNode t = new TreeNode(elements[0]);
      t.right = _current.right;
      _current.right = t;
      // _current.right = recursiveAdd(_current.right, _path, _object, _data);
      recursiveAdd(_current.right, _path, _object, _data);
    }
    // recurse right
    else {
      // _current.right = recursiveAdd(_current.right, _path, _object, _data);
      recursiveAdd(_current.right, _path, _object, _data);
    }
    return _current;
  }

  public synchronized void add(String _path, ExportedObject _object, String _data) {
    LOGGER.debug("Adding {} to object tree", _path);
    // root = recursiveAdd(root, _path, _object, _data);
    recursiveAdd(root, _path, _object, _data);
  }

  public synchronized void remove(String _path) {
    LOGGER.debug("Removing {} from object tree", _path);
    TreeNode t = recursiveFind(root, _path);
    assert t != null;
    t.object = null;
    t.data = null;
  }

  // CHECKSTYLE:OFF
  public String Introspect(String _path) {
    // CHECKSTYLE:ON
    TreeNode t = recursiveFind(root, _path);
    if (null == t) {
      return null;
    }
    StringBuilder sb = new StringBuilder();

    sb.append("<node name=\"");
    sb.append(_path);
    sb.append("\">\n");

    if (null != t.data) {
      sb.append(t.data);
    }
    t = t.down;
    while (null != t) {
      // omit entries without a bound object
      // if there is no object, there is nothing to show in introspection
      // also  non exported object will then be removed from introspection output
      if (t.object == null) {
        t = t.right;
        continue;
      }
      sb.append("<node name=\"");
      sb.append(t.name);
      sb.append("\"/>\n");
      t = t.right;
    }
    sb.append("</node>");
    return sb.toString();
  }

  private String recursivePrint(TreeNode _current) {
    String s = "";
    if (null != _current) {
      s += _current.name;
      if (null != _current.object) {
        s += "*";
      }
      if (null != _current.down) {
        s += "/{" + recursivePrint(_current.down) + "}";
      }
      if (null != _current.right) {
        s += ", " + recursivePrint(_current.right);
      }
    }
    return s;
  }

  @Override
  public String toString() {
    return recursivePrint(root);
  }
}
