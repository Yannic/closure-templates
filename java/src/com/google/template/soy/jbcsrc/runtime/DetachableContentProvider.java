/*
 * Copyright 2015 Google Inc.
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

package com.google.template.soy.jbcsrc.runtime;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.template.soy.data.LogStatement;
import com.google.template.soy.data.LoggingAdvisingAppendable;
import com.google.template.soy.data.LoggingAdvisingAppendable.BufferingAppendable;
import com.google.template.soy.data.LoggingFunctionInvocation;
import com.google.template.soy.data.SanitizedContent.ContentKind;
import com.google.template.soy.data.SoyValue;
import com.google.template.soy.data.SoyValueProvider;
import com.google.template.soy.data.restricted.BooleanData;
import com.google.template.soy.jbcsrc.api.RenderResult;
import com.google.template.soy.jbcsrc.shared.StackFrame;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;

/**
 * A special implementation of {@link SoyValueProvider} to use as a shared base class for the {@code
 * jbcsrc} implementations of the generated {@code LetContentNode} and {@code CallParamContentNode}
 * implementations.
 */
public abstract class DetachableContentProvider extends SoyValueProvider {

  // Will be either a SanitizedContent, a StringData.
  private SoyValue resolvedValue;

  private final MultiplexingAppendable appendable;
  private StackFrame frame;

  protected DetachableContentProvider(ContentKind kind) {
    this.appendable = MultiplexingAppendable.create(kind);
  }

  // This is called for an 'optimistic' evaluation when it fails.
  protected DetachableContentProvider(StackFrame frame, MultiplexingAppendable appendable) {
    this.appendable = appendable;
    this.frame = frame;
  }

  @Override
  public final SoyValue resolve() {
    SoyValue local = resolvedValue;
    if (local == null) {
      JbcSrcRuntime.awaitProvider(this);
      local = resolvedValue;
    }
    return local;
  }

  @Override
  public final RenderResult status() {
    if (isDone()) {
      return RenderResult.done();
    }
    var localAppendable = this.appendable;
    StackFrame local = frame = doRender(frame, localAppendable);

    if (local == null) {
      // following a .status() call the most likely thing is resolve, just do it now
      resolvedValue = localAppendable.getAsSoyValue();
      return RenderResult.done();
    }
    return local.asRenderResult();
  }

  @Override
  public final RenderResult renderAndResolve(LoggingAdvisingAppendable appendable)
      throws IOException {
    if (isDone()) {
      this.appendable.replayFinishedOn(appendable);
      return RenderResult.done();
    }

    var localAppendable = this.appendable;
    int delegateIndex = localAppendable.addDelegate(appendable);
    StackFrame local = frame = doRender(frame, localAppendable);
    if (local == null) {
      resolvedValue = localAppendable.getAsSoyValue();
      localAppendable.removeDelegate(delegateIndex, appendable);
      return RenderResult.done();
    }
    return local.asRenderResult();
  }

  @Override
  public SoyValueProvider coerceToBooleanProvider() {
    if (resolvedValue != null) {
      // If already resolved, coerce to boolean.
      return BooleanData.forValue(resolvedValue.coerceToBoolean());
    }

    var kind = appendable.getSanitizedContentKind();
    if (kind != ContentKind.TEXT) {
      // SanitizedContent is always truthy.
      return BooleanData.TRUE;
    }
    if (!appendable.isEmpty()) {
      return BooleanData.TRUE;
    }

    var delegate = this;

    // Fall back to evaluating truthy as soon as there's any content.
    return new SoyValueProvider() {
      private BooleanData resolvedValue;

      @Override
      public SoyValue resolve() {
        if (resolvedValue != null) {
          return resolvedValue;
        }
        JbcSrcRuntime.awaitProvider(this);
        return resolvedValue;
      }

      @Override
      public RenderResult status() {
        var status = delegate.status();

        if (status.isDone()) {
          resolvedValue = BooleanData.forValue(delegate.resolve().coerceToBoolean());
          return RenderResult.done();
        }
        if (!delegate.appendable.isEmpty()) {
          resolvedValue = BooleanData.TRUE;
          return RenderResult.done();
        }
        return status;
      }

      @Override
      public RenderResult renderAndResolve(LoggingAdvisingAppendable appendable)
          throws IOException {
        RenderResult result = status();
        if (result.isDone()) {
          resolve().render(appendable);
        }
        return result;
      }
    };
  }

  private boolean isDone() {
    return resolvedValue != null;
  }

  /** Overridden by generated subclasses to implement lazy detachable resolution. */
  @Nullable
  protected abstract StackFrame doRender(
      @Nullable StackFrame frame, MultiplexingAppendable appendable);

  /**
   * An {@link AdvisingAppendable} that forwards to a set of delegate appendables but also saves all
   * the same forwarded content into a buffer.
   */
  public static final class MultiplexingAppendable extends BufferingAppendable {

    public static MultiplexingAppendable create(ContentKind kind) {
      return new MultiplexingAppendable(kind);
    }

    private MultiplexingAppendable(ContentKind kind) {
      super(kind);
    }

    // Lazily initialized to avoid allocations in the common case of optimistic evaluation
    // succeeding or there being no delegates (because we're capturing as a SoyValue).
    // The second most common case is a single delegate,  so we could consider special casing that
    // but it is probably not worth it and would add a lot of complexity.
    List<LoggingAdvisingAppendable> delegates;

    int addDelegate(LoggingAdvisingAppendable delegate) throws IOException {
      // We don't want to add the same delegate multiple times.
      // a linear scan is sufficient since typically there is only one delegate
      var delegates = this.delegates;
      if (delegates == null) {
        delegates = new ArrayList<>();
        delegates.add(delegate);
        this.delegates = delegates;
        super.replayOn(delegate);
        return 0;
      }
      int index = delegates.indexOf(delegate);
      if (index == -1) {
        index = delegates.size();
        super.replayOn(delegate);
        delegates.add(delegate);
      }
      return index;
    }

    void replayFinishedOn(LoggingAdvisingAppendable appendable) throws IOException {
      var delegates = this.delegates;
      if (delegates != null && delegates.remove(appendable)) {
        return;
      }
      super.replayOn(appendable);
    }

    void removeDelegate(int delegateIndex, LoggingAdvisingAppendable expected) {
      var actual = delegates.remove(delegateIndex);
      if (actual != expected) {
        throw new AssertionError("impossible");
      }
    }

    @CanIgnoreReturnValue
    @Override
    public LoggingAdvisingAppendable append(CharSequence csq) throws IOException {
      super.append(csq);
      var delegates = this.delegates;
      if (delegates == null) {
        return this;
      }
      int size = delegates.size();
      for (int i = 0; i < size; i++) {
        delegates.get(i).append(csq);
      }
      return this;
    }

    @CanIgnoreReturnValue
    @Override
    public LoggingAdvisingAppendable append(CharSequence csq, int start, int end)
        throws IOException {
      super.append(csq, start, end);
      var delegates = this.delegates;
      if (delegates == null) {
        return this;
      }
      int size = delegates.size();
      for (int i = 0; i < size; i++) {
        delegates.get(i).append(csq, start, end);
      }
      return this;
    }

    @Override
    public LoggingAdvisingAppendable append(char c) throws IOException {
      super.append(c);
      var delegates = this.delegates;
      if (delegates == null) {
        return this;
      }
      int size = delegates.size();
      for (int i = 0; i < size; i++) {
        delegates.get(i).append(c);
      }
      return this;
    }

    @Override
    public boolean softLimitReached() {
      // don't need to query our base class, it always returns false
      var delegates = this.delegates;
      if (delegates == null) {
        return false;
      }
      int size = delegates.size();
      for (int i = 0; i < size; i++) {
        if (delegates.get(i).softLimitReached()) {
          return true;
        }
      }
      return false;
    }

    @Override
    public void flushBuffers(int depth) {
      // This is a 'root' appendable so while we have a delegate there should never be a case where
      // we need to be flushed through.
      throw new AssertionError("should not be called");
    }

    @CanIgnoreReturnValue
    @Override
    public LoggingAdvisingAppendable enterLoggableElement(LogStatement statement) {
      super.enterLoggableElement(statement);
      var delegates = this.delegates;
      if (delegates == null) {
        return this;
      }
      int size = delegates.size();
      for (int i = 0; i < size; i++) {
        delegates.get(i).enterLoggableElement(statement);
      }
      return this;
    }

    @CanIgnoreReturnValue
    @Override
    public LoggingAdvisingAppendable exitLoggableElement() {
      super.exitLoggableElement();
      var delegates = this.delegates;
      if (delegates == null) {
        return this;
      }
      int size = delegates.size();
      for (int i = 0; i < size; i++) {
        delegates.get(i).exitLoggableElement();
      }
      return this;
    }

    @CanIgnoreReturnValue
    @Override
    public LoggingAdvisingAppendable appendLoggingFunctionInvocation(
        LoggingFunctionInvocation funCall, ImmutableList<Function<String, String>> escapers)
        throws IOException {
      super.appendLoggingFunctionInvocation(funCall, escapers);
      var delegates = this.delegates;
      if (delegates == null) {
        return this;
      }
      int size = delegates.size();
      for (int i = 0; i < size; i++) {
        delegates.get(i).appendLoggingFunctionInvocation(funCall, escapers);
      }
      return this;
    }
  }
}
