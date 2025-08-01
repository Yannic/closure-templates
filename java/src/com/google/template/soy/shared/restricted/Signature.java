/*
 * Copyright 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.template.soy.shared.restricted;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that represents parameter types and return type for a particular function signature.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Signature {
  /**
   * Defines the positional parameter types of the parameters as strings using the Soy type syntax.
   */
  String[] parameterTypes() default {};

  /** Defines the return type of the function using the Soy type syntax. */
  String returnType();

  /** Allows specifying a particular overload as deprecated. */
  String deprecatedWarning() default "";

  /** Whether to handle the last parameter as a varargs parameter. */
  boolean isVarArgs() default false;
}
