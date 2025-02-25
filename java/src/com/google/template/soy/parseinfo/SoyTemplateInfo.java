/*
 * Copyright 2009 Google Inc.
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

package com.google.template.soy.parseinfo;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.Immutable;

/** Parsed info about a template. */
@Immutable
public class SoyTemplateInfo {

  /** Enum for whether a param is required or optional for a specific template. */
  public enum ParamRequisiteness {
    REQUIRED,
    OPTIONAL
  }

  /** The full template name. */
  private final String name;

  private final TemplateName templateName;

  /** Map from each param to whether it's required for this template. */
  private final ImmutableMap<String, ParamRequisiteness> paramMap;

  /**
   * Constructor for internal use only. Do not call, do not subclass.
   *
   * @param name The full template name.
   * @param paramMap Map from each param to whether it's required for this template.
   */
  protected SoyTemplateInfo(
      String name, TemplateName templateName, ImmutableMap<String, ParamRequisiteness> paramMap) {
    Preconditions.checkArgument(name.lastIndexOf('.') > 0);
    this.name = name;
    this.templateName = templateName;
    this.paramMap = paramMap;
  }

  /**
   * Returns the full template name, e.g. {@code myNamespace.myTemplate}.
   *
   * @deprecated Use {@link #getTemplateName()} instead. Treating template names as opaque strings
   *     is safer and preferred.
   */
  @Deprecated
  public final String getName() {
    return name;
  }

  /** Returns the full template name. */
  public final TemplateName getTemplateName() {
    return Preconditions.checkNotNull(templateName);
  }

  /** Returns the partial template name (starting from the last dot), e.g. {@code .myTemplate}. */
  public final String getPartialName() {
    String name = getName();
    return name.substring(name.lastIndexOf('.'));
  }

  /** Returns the number of parameters in this template. */
  public final int getParamCount() {
    return paramMap.size();
  }

  /** Returns the set of parameter names in this template. */
  public final ImmutableSet<String> getParamNames() {
    return paramMap.keySet();
  }

  /**
   * Returns the set of parameter names in this template for which {@link #getParamRequisiteness}
   * returns {@link ParamRequisiteness#REQUIRED}.
   */
  public final ImmutableSet<String> getRequiredParamNames() {
    return paramMap.keySet().stream()
        .filter(n -> paramMap.get(n) == ParamRequisiteness.REQUIRED)
        .collect(toImmutableSet());
  }

  /** Returns true if this template has a parameter names {@code paramName}. */
  public final boolean hasParam(String paramName) {
    return getParamNames().contains(paramName);
  }

  /** Returns whether {@code paramName} is required, optional, or indirect. */
  public final ParamRequisiteness getParamRequisiteness(String paramName) {
    ParamRequisiteness pr = paramMap.get(paramName);
    Preconditions.checkArgument(pr != null);
    return pr;
  }
}
