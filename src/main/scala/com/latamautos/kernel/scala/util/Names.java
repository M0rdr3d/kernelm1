package com.latamautos.kernel.scala.util;

public class Names {

  private Names() {}

  public static Named named(Class<?> name) {
    return new NamedImpl(name);
  }
}
