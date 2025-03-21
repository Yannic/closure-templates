/*
 * Copyright 2011 Google Inc.
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

package com.google.template.soy.sharedpasses.render;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import com.google.template.soy.base.SourceLocation;
import com.google.template.soy.soytree.SoyNode;
import com.google.template.soy.soytree.SoyNode.StackContextNode;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.annotation.Nullable;

/** Exception thrown when a rendering or evaluation attempt fails. */
public final class RenderException extends RuntimeException {

  public static RenderException create(String message) {
    return create(message, null);
  }

  public static RenderException create(String message, Throwable cause) {
    return new RenderException(message, cause);
  }

  @FormatMethod
  public static RenderException createF(@FormatString String format, Object... args) {
    return create(String.format(format, args), null);
  }

  @FormatMethod
  public static RenderException createF(
      @FormatString String format, Throwable cause, Object... args) {
    return create(String.format(format, args), cause);
  }

  public static RenderException createWithSource(String message, SoyNode source) {
    return createWithSource(message, null, source);
  }

  public static RenderException createWithSource(
      String message, @Nullable Throwable cause, SoyNode source) {
    return new RenderException(message, cause).addStackTraceElement(source);
  }

  public static RenderException createFromRenderException(
      String message, RenderException cause, SoyNode node) {
    RenderException renderException = new RenderException(message, cause.getCause());
    renderException.soyStackTrace.addAll(cause.soyStackTrace);
    renderException.addStackTraceElement(node);
    return renderException;
  }

  /** The list of all stack traces from the soy rendering. */
  private final Deque<StackTraceElement> soyStackTrace = new ArrayDeque<>();

  /**
   * @param message A detailed description of the error.
   * @param cause The underlying error.
   */
  private RenderException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public synchronized Throwable fillInStackTrace() {
    // Remove java stack trace, we only care about the soy stack.
    return this;
  }

  /** Add a partial stack trace element by specifying the source location of the soy file. */
  RenderException addStackTraceElement(SoyNode node) {
    // Typically, this is fast since templates aren't that deep and we only do this in error
    // situations so performance matters less.
    StackContextNode template = node.getNearestAncestor(StackContextNode.class);
    if (template != null) {
      return addStackTraceElement(template, node.getSourceLocation());
    }
    return this;
  }

  /** Add a partial stack trace element by specifying the source location of the soy file. */
  @CanIgnoreReturnValue
  RenderException addStackTraceElement(StackContextNode template, SourceLocation location) {
    // Typically, this is fast since templates aren't that deep and we only do this in error
    // situations so performance matters less.
    soyStackTrace.add(template.createStackTraceElement(location));
    return this;
  }

  /** Finalize the stack trace by prepending the soy stack trace to the given Throwable. */
  public void finalizeStackTrace(Throwable t) {
    t.setStackTrace(concatWithJavaStackTrace(t.getStackTrace()));
  }

  /**
   * Prepend the soy stack trace to the given standard java stack trace.
   *
   * @param javaStackTrace The java stack trace to prepend. This should come from
   *     Throwable#getStackTrace()
   * @return The combined stack trace to use. Callers should call Throwable#setStackTrace() to
   *     override another Throwable's stack trace.
   */
  private StackTraceElement[] concatWithJavaStackTrace(StackTraceElement[] javaStackTrace) {
    if (soyStackTrace.isEmpty()) {
      return javaStackTrace;
    }

    StackTraceElement[] finalStackTrace =
        new StackTraceElement[soyStackTrace.size() + javaStackTrace.length];
    soyStackTrace.toArray(finalStackTrace);
    System.arraycopy(
        javaStackTrace, 0, finalStackTrace, soyStackTrace.size(), javaStackTrace.length);
    return finalStackTrace;
  }
}
