/*
 * Copyright 2026 Google Inc.
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

package com.google.template.soy.types;

import com.google.template.soy.soytree.SoyTypeP;

/**
 * A special Soy type designed to help upstream compilers that both lift lambas into file-level
 * templates or externs AND lack type checking.
 *
 * <p>This type is allowed only as the type of a template or extern parameter or as the return type
 * of an extern.
 *
 * <p>Templates and externs that use this type must be called exactly once from within the file in
 * which they are declared. The compiler will infer the actual type from the single call site. If
 * there are multiple calls to the same template or extern, or if there are none, the compiler will
 * report an error.
 */
public final class ImplicitType extends PrimitiveType {

  private static final ImplicitType INSTANCE = new ImplicitType();

  private ImplicitType() {}

  @Override
  public Kind getKind() {
    return Kind.IMPLICIT;
  }

  @Override
  public String toString() {
    return "implicit";
  }

  @Override
  protected void doToProto(SoyTypeP.Builder builder) {
    throw new UnsupportedOperationException();
  }

  /** Return the single instance of this type. */
  public static ImplicitType getInstance() {
    return INSTANCE;
  }
}
