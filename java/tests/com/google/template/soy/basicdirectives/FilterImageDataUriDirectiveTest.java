/*
 * Copyright 2013 Google Inc.
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

package com.google.template.soy.basicdirectives;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.template.soy.data.SanitizedContent;
import com.google.template.soy.data.SanitizedContent.ContentKind;
import com.google.template.soy.data.UnsafeSanitizedContentOrdainer;
import com.google.template.soy.jssrc.dsl.Expression;
import com.google.template.soy.jssrc.dsl.Expressions;
import com.google.template.soy.jssrc.dsl.FormatOptions;
import com.google.template.soy.pysrc.restricted.PyExpr;
import com.google.template.soy.testing.AbstractSoyPrintDirectiveTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link FilterImageDataUriDirective}. */
@RunWith(JUnit4.class)
public class FilterImageDataUriDirectiveTest extends AbstractSoyPrintDirectiveTestCase {
  @Test
  public void testApplyForTofu() {
    FilterImageDataUriDirective directive = new FilterImageDataUriDirective();
    // More comprehensive tests in SanitizersTest.
    assertTofuOutput(
        sanitizedUri("data:image/png;base64,abc="), "data:image/png;base64,abc=", directive);
    assertTofuOutput(sanitizedUri("data:image/gif;base64,zSoyz"), "not really valid", directive);
  }

  @Test
  public void testApplyForJsSrc() {
    FilterImageDataUriDirective cleanHtml = new FilterImageDataUriDirective();
    Expression dataRef = Expressions.dottedIdNoRequire("opt_data.myKey");
    assertThat(cleanHtml.applyForJsSrc(dataRef, ImmutableList.of()).getCode(FormatOptions.JSSRC))
        .isEqualTo("soy.$$filterImageDataUri(opt_data.myKey);");
  }

  @Test
  public void testApplyForPySrc() {
    FilterImageDataUriDirective cleanHtml = new FilterImageDataUriDirective();
    PyExpr data = new PyExpr("'data'", Integer.MAX_VALUE);
    assertThat(cleanHtml.applyForPySrc(data, ImmutableList.of()).getText())
        .isEqualTo("sanitize.filter_image_data_uri('data')");
  }

  private SanitizedContent sanitizedUri(String s) {
    return UnsafeSanitizedContentOrdainer.ordainAsSafe(s, ContentKind.URI);
  }
}
