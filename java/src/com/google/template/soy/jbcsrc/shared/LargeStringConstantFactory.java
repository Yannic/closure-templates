/*
 * Copyright 2021 Google Inc.
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

package com.google.template.soy.jbcsrc.shared;

import java.lang.invoke.MethodHandles;

/**
 * An {@code constantdynamic} bootstrap for handling large string constants.
 *
 * <p>In soy it is not unreasonable for there to be very large string constants (>65k utf8 bytes),
 * for example, consider a very large {@code {literal}} inclusion, or an inline base64 encoded
 * image. These scenarios are problematic for the class file format since string constants are
 * limited to 65K bytes. This factory can help bridge the difference.
 *
 * <p>The benefit of using {@code constantdynamic} to concatenate strings as opposed to just
 * generating code that performs it inline is that we can ensure that the concatenation is only
 * performed once (and lazily) without needing to allocate {@code static final} fields to hold the
 * result.
 */
public final class LargeStringConstantFactory {
  public static String bootstrapLargeStringConstant(
      MethodHandles.Lookup lookup, String name, Class<?> type, String... parts) {
    int size = 0;
    for (String part : parts) {
      size += part.length();
    }
    StringBuilder sb = new StringBuilder(size);
    for (String part : parts) {
      sb.append(part);
    }

    // Return a constant method handle.  All future invocations will just return the string value.
    return sb.toString();
  }

  private LargeStringConstantFactory() {}
}
