/*
 * Copyright 2017 Google Inc.
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

package com.google.template.soy.jbcsrc.api;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.html.types.SafeHtml;
import com.google.template.soy.data.LogStatement;
import com.google.template.soy.data.LoggingFunctionInvocation;
import com.google.template.soy.logging.SoyLogger;
import java.io.IOException;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link OutputAppendableTest} */
@RunWith(JUnit4.class)
public final class OutputAppendableTest {

  private static final LogStatement LOGONLY = LogStatement.create(1, null, /* logOnly= */ true);
  private static final LogStatement NOT_LOGONLY =
      LogStatement.create(1, null, /* logOnly= */ false);
  private static final LogStatement LOGGING_ATTRS =
      LogStatement.create(2, null, /* logOnly= */ false);

  private static final SoyLogger LOGGER =
      new SoyLogger() {
        @Override
        public Optional<SafeHtml> exit() {
          return Optional.empty();
        }

        @Override
        public String evalLoggingFunction(LoggingFunctionInvocation value) {
          return value.placeholderValue();
        }

        @Override
        public EnterData enter(LogStatement statement) {
          if (statement == LOGGING_ATTRS) {
            return EnterData.create(
                LoggingAttrs.builder().addDataAttribute("data-foo", "bar").build());
          }
          return EnterData.EMPTY;
        }
      };

  @Test
  public void testLogonly_logonly_above_regular() throws IOException {
    var buffer = new StringBuilder();
    OutputAppendable buffering = OutputAppendable.create(buffer, LOGGER);
    buffering.append("a");
    buffering.enterLoggableElement(LOGONLY);
    buffering.append("b");
    buffering.enterLoggableElement(NOT_LOGONLY);
    buffering.append("c");
    buffering.exitLoggableElement();
    buffering.append("d");
    buffering.exitLoggableElement();
    buffering.append("e");
    assertThat(buffer.toString()).isEqualTo("ae");
  }

  @Test
  public void testLogonly_regular_above_logong() throws IOException {
    var buffer = new StringBuilder();
    OutputAppendable buffering = OutputAppendable.create(buffer, LOGGER);
    buffering.append("a");
    buffering.enterLoggableElement(NOT_LOGONLY);
    buffering.append("b");
    buffering.enterLoggableElement(LOGONLY);
    buffering.append("c");
    buffering.exitLoggableElement();
    buffering.append("d");
    buffering.exitLoggableElement();
    buffering.append("e");
    assertThat(buffer.toString()).isEqualTo("abde");
  }

  @Test
  public void testLogonly_logonly_before_regular() throws IOException {
    var buffer = new StringBuilder();
    OutputAppendable buffering = OutputAppendable.create(buffer, LOGGER);
    buffering.enterLoggableElement(LOGONLY);
    buffering.append("logonly");
    buffering.exitLoggableElement();
    buffering.enterLoggableElement(NOT_LOGONLY);
    buffering.append("hello");
    buffering.exitLoggableElement();
    assertThat(buffer.toString()).isEqualTo("hello");
  }

  @Test
  public void testLogonly_logonly_after_regular() throws IOException {
    var buffer = new StringBuilder();
    OutputAppendable buffering = OutputAppendable.create(buffer, LOGGER);
    buffering.enterLoggableElement(NOT_LOGONLY);
    buffering.append("hello");
    buffering.exitLoggableElement();
    buffering.enterLoggableElement(LOGONLY);
    buffering.append("logonly");
    buffering.exitLoggableElement();
    assertThat(buffer.toString()).isEqualTo("hello");
  }

  @Test
  public void testLogonly_deeplyNested() throws IOException {
    var buffer = new StringBuilder();
    OutputAppendable buffering = OutputAppendable.create(buffer, LOGGER);
    buffering.append("a");
    for (int i = 0; i < 1024; i++) {
      buffering.enterLoggableElement(LOGONLY);
      buffering.append("b");
      buffering.exitLoggableElement();
    }
    buffering.append("c");
    assertThat(buffer.toString()).isEqualTo("ac");
  }

  @Test
  public void testAppliesEscapersToPlaceholder() throws IOException {
    var buffer = new StringBuilder();
    OutputAppendable buffering = OutputAppendable.create(buffer, LOGGER);
    buffering.appendLoggingFunctionInvocation(
        LoggingFunctionInvocation.create("foo", "placeholder", ImmutableList.of()),
        ImmutableList.of(Functions.forMap(ImmutableMap.of("placeholder", "replacement"))));
    assertThat(buffer.toString()).isEqualTo("replacement");
  }

  @Test
  public void testLoggingAttrs_anchor() throws IOException {
    var buffer = new StringBuilder();
    OutputAppendable buffering = OutputAppendable.create(buffer, LOGGER);
    buffering.append("<a").flushPendingLoggingAttributes(true).append("></a>");
    assertThat(buffer.toString()).isEqualTo("<a></a>");
    buffer.setLength(0);
    buffering
        .enterLoggableElement(LOGGING_ATTRS)
        .append("<a")
        .flushPendingLoggingAttributes(false)
        .append("></a>")
        .exitLoggableElement();
    assertThat(buffer.toString()).isEqualTo("<a data-foo=\"bar\"></a>");
  }
}
