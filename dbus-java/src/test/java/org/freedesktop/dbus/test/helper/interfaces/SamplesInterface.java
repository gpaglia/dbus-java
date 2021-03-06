package org.freedesktop.dbus.test.helper.interfaces;

import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.annotations.IntrospectionDescription;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.test.helper.interfaces.Binding.CrossSampleStruct;
import org.freedesktop.dbus.test.helper.interfaces.Binding.Triplet;
import org.freedesktop.dbus.types.UInt16;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.UInt64;
import org.freedesktop.dbus.types.Variant;

public interface SamplesInterface extends DBusInterface {
  @SuppressWarnings("unused")
  @IntrospectionDescription("Returns whatever it is passed")
  <T> Variant<T> Identity(Variant<T> input);

  @SuppressWarnings("unused")
  @IntrospectionDescription("Returns whatever it is passed")
  byte IdentityByte(byte input);

  @SuppressWarnings("unused")
  @IntrospectionDescription("Returns whatever it is passed")
  boolean IdentityBool(boolean input);

  @SuppressWarnings("unused")
  @IntrospectionDescription("Returns whatever it is passed")
  short IdentityInt16(short input);

  @SuppressWarnings("unused")
  @IntrospectionDescription("Returns whatever it is passed")
  UInt16 IdentityUInt16(UInt16 input);

  @SuppressWarnings("unused")
  @IntrospectionDescription("Returns whatever it is passed")
  int IdentityInt32(int input);

  @SuppressWarnings("unused")
  @IntrospectionDescription("Returns whatever it is passed")
  UInt32 IdentityUInt32(UInt32 input);

  @SuppressWarnings("unused")
  @IntrospectionDescription("Returns whatever it is passed")
  long IdentityInt64(long input);

  @SuppressWarnings("unused")
  @IntrospectionDescription("Returns whatever it is passed")
  UInt64 IdentityUInt64(UInt64 input);

  @SuppressWarnings("unused")
  @IntrospectionDescription("Returns whatever it is passed")
  double IdentityDouble(double input);

  @SuppressWarnings("unused")
  @IntrospectionDescription("Returns whatever it is passed")
  String IdentityString(String input);

  @SuppressWarnings("unused")
  @IntrospectionDescription("Returns whatever it is passed")
  <T> Variant<T>[] IdentityArray(Variant<T>[] input);

  @SuppressWarnings("unused")
  @IntrospectionDescription("Returns whatever it is passed")
  byte[] IdentityByteArray(byte[] input);

  @SuppressWarnings("unused")
  @IntrospectionDescription("Returns whatever it is passed")
  boolean[] IdentityBoolArray(boolean[] input);

  @SuppressWarnings("unused")
  @IntrospectionDescription("Returns whatever it is passed")
  short[] IdentityInt16Array(short[] input);

  @SuppressWarnings("unused")
  @IntrospectionDescription("Returns whatever it is passed")
  UInt16[] IdentityUInt16Array(UInt16[] input);

  @SuppressWarnings("unused")
  @IntrospectionDescription("Returns whatever it is passed")
  int[] IdentityInt32Array(int[] input);

  @SuppressWarnings("unused")
  @IntrospectionDescription("Returns whatever it is passed")
  UInt32[] IdentityUInt32Array(UInt32[] input);

  @SuppressWarnings("unused")
  @IntrospectionDescription("Returns whatever it is passed")
  long[] IdentityInt64Array(long[] input);

  @SuppressWarnings("unused")
  @IntrospectionDescription("Returns whatever it is passed")
  UInt64[] IdentityUInt64Array(UInt64[] input);

  @SuppressWarnings("unused")
  @IntrospectionDescription("Returns whatever it is passed")
  double[] IdentityDoubleArray(double[] input);

  @SuppressWarnings("unused")
  @IntrospectionDescription("Returns whatever it is passed")
  String[] IdentityStringArray(String[] input);

  @SuppressWarnings("unused")
  @IntrospectionDescription("Returns the sum of the values in the input list")
  long Sum(int[] a);

  @SuppressWarnings("unused")
  @IntrospectionDescription("Given a map of A => B, should return a map of B => a list of all the As which mapped to B")
  Map<String, List<String>> InvertMapping(Map<String, String> a);

  @SuppressWarnings("unused")
  @IntrospectionDescription("This method returns the contents of a struct as separate values")
  Triplet<String, UInt32, Short> DeStruct(CrossSampleStruct a);

  @IntrospectionDescription("Given any compound type as a variant, return all the primitive types recursively contained within as an array of variants")
  List<Variant<Object>> Primitize(Variant<Object> a);

  @SuppressWarnings("unused")
  @IntrospectionDescription("inverts it's input")
  boolean Invert(boolean a);

  @SuppressWarnings("unused")
  @IntrospectionDescription("triggers sending of a signal from the supplied object with the given parameter")
  void Trigger(String a, UInt64 b);

  @SuppressWarnings("unused")
  @IntrospectionDescription("Causes the server to exit")
  void Exit();
}