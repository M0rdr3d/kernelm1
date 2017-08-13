/**
 * Copyright (C) 2006 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.latamautos.kernel.scala.util;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotates named things.
 *
 * @author crazybob@google.com (Bob Lee)
 */
@Retention(RUNTIME)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD })
@BindingAnnotation
//@interface Decorated {
//  Class<? extends PropertyDecorator> decorator();
//}
//
//interface PropertyDecorator {
//  String decorate(String value);
//}
//
//class TitleCaseDecorator implements PropertyDecorator {
//  @Override
//  public String decorate(String value) {
//    return value;
//  }
//}

public @interface Named {
//  @Decorated(decorator = TitleCaseDecorator.class)
  Class<?> value();
}
