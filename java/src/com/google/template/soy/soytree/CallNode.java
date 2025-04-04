/*
 * Copyright 2008 Google Inc.
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

package com.google.template.soy.soytree;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.template.soy.base.SourceLocation.UNKNOWN;
import static com.google.template.soy.base.internal.BaseUtils.convertToUpperUnderscore;
import static com.google.template.soy.soytree.CommandTagAttribute.INVALID_ATTRIBUTE;
import static com.google.template.soy.soytree.MessagePlaceholder.PHEX_ATTR;
import static com.google.template.soy.soytree.MessagePlaceholder.PHNAME_ATTR;
import static com.google.template.soy.soytree.MessagePlaceholder.validatePlaceholderExample;
import static com.google.template.soy.soytree.MessagePlaceholder.validatePlaceholderName;

import com.google.common.collect.ImmutableList;
import com.google.template.soy.base.SourceLocation;
import com.google.template.soy.base.internal.Identifier;
import com.google.template.soy.base.internal.QuoteStyle;
import com.google.template.soy.basetree.CopyState;
import com.google.template.soy.error.ErrorReporter;
import com.google.template.soy.exprtree.ExprRootNode;
import com.google.template.soy.shared.restricted.SoyPrintDirective;
import com.google.template.soy.soytree.CommandTagAttribute.CommandTagAttributesHolder;
import com.google.template.soy.soytree.SoyNode.ExprHolderNode;
import com.google.template.soy.soytree.SoyNode.MsgPlaceholderInitialNode;
import com.google.template.soy.soytree.SoyNode.SplitLevelTopNode;
import com.google.template.soy.soytree.SoyNode.StandaloneNode;
import com.google.template.soy.soytree.SoyNode.StatementNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

/** Node representing a call. */
public abstract class CallNode extends AbstractParentCommandNode<CallParamNode>
    implements StandaloneNode,
        SplitLevelTopNode<CallParamNode>,
        StatementNode,
        HtmlContext.HtmlContextHolder,
        ExprHolderNode,
        MsgPlaceholderInitialNode,
        CommandTagAttributesHolder {

  /** Fallback base placeholder name. */
  private static final String FALLBACK_BASE_PLACEHOLDER_NAME = "XXX";

  static final String KEY = "key";
  static final String DATA = "data";
  static final String ERROR_FALLBACK = "errorfallback";

  /** True if this call is passing data="all". */
  private boolean isPassingAllData;

  /** Used for formatting */
  private final boolean selfClosing;

  /** Used for formatting */
  private ImmutableList<CommandTagAttribute> attributes;

  private final SourceLocation openTagLocation;

  private final MessagePlaceholder placeholder;

  /** The HTML context that the call is in, such as in HTML or Attributes. */
  @Nullable private HtmlContext htmlContext;

  /**
   * The call key, which is the encompassing template name along with position in template. This is
   * used to help with dom alignment in Incremental DOM backend.
   */
  @Nullable private String callKey;

  /**
   * Escaping directives to apply to the return value. With strict autoescaping, the result of each
   * call site is escaped, which is potentially a no-op if the template's return value is the
   * correct SanitizedContent object.
   *
   * <p>Set by the contextual rewriter.
   */
  private ImmutableList<SoyPrintDirective> escapingDirectives = ImmutableList.of();

  /** True if this node is within a HTML context. */
  private boolean isPcData = false;

  private final boolean errorFallbackSkip;

  /** Protected constructor for use by subclasses. */
  protected CallNode(
      int id,
      SourceLocation location,
      SourceLocation openTagLocation,
      String commandName,
      List<CommandTagAttribute> attributes,
      boolean selfClosing,
      ErrorReporter reporter) {
    super(id, location, commandName);

    SourceLocation phNameLocation = null;
    String phName = null;
    Optional<String> phExample = Optional.empty();
    boolean errorFallbackSkip = false;
    for (CommandTagAttribute attr : attributes) {
      String name = attr.getName().identifier();

      switch (name) {
        case DATA:
          if (!attr.hasExprValue() && "all".equals(attr.getValue())) {
            this.isPassingAllData = true;
          } else {
            this.isPassingAllData = false;
            attr.checkAsExpr(reporter); // report any errors to compiler
          }
          break;
        case KEY:
          attr.checkAsExpr(reporter); // report any errors to compiler
          break;
        case PHNAME_ATTR:
          phNameLocation = attr.getValueLocation();
          phName = validatePlaceholderName(attr.getValue(), phNameLocation, reporter);
          break;
        case PHEX_ATTR:
          phExample =
              Optional.ofNullable(
                  validatePlaceholderExample(attr.getValue(), attr.getValueLocation(), reporter));
          break;
        case ERROR_FALLBACK:
          if (attr.getValue().equals("skip")) {
            errorFallbackSkip = true;
          } else {
            reporter.report(attr.getSourceLocation(), INVALID_ATTRIBUTE, ERROR_FALLBACK, "skip");
          }
          break;
        default:
          // do nothing, validated by subclasses
      }
    }

    this.attributes = ImmutableList.copyOf(attributes);
    this.selfClosing = selfClosing;
    this.placeholder =
        (phName == null)
            ? MessagePlaceholder.create(FALLBACK_BASE_PLACEHOLDER_NAME, phExample)
            : MessagePlaceholder.createWithUserSuppliedName(
                convertToUpperUnderscore(phName), phName, phNameLocation, phExample);
    this.openTagLocation = openTagLocation;
    this.errorFallbackSkip = errorFallbackSkip;
  }

  /**
   * Copy constructor.
   *
   * @param orig The node to copy.
   */
  protected CallNode(CallNode orig, CopyState copyState) {
    super(orig, copyState);
    this.isPassingAllData = orig.isPassingAllData;
    this.placeholder = orig.placeholder;
    this.escapingDirectives = orig.escapingDirectives;
    this.callKey = orig.callKey;
    this.isPcData = orig.getIsPcData();
    this.htmlContext = orig.htmlContext;
    this.openTagLocation = orig.openTagLocation;
    this.selfClosing = orig.selfClosing;
    this.attributes =
        orig.attributes.stream().map(c -> c.copy(copyState)).collect(toImmutableList());
    this.errorFallbackSkip = orig.errorFallbackSkip;
    // we may have handed out a copy to ourselves via genSamenessKey()
    copyState.updateRefs(orig, this);
  }

  /**
   * Gets the HTML source context immediately prior to the node (typically tag, attribute value,
   * HTML PCDATA, or plain text) which this node emits in. This affects how the node is escaped (for
   * traditional backends) or how it's passed to incremental DOM APIs.
   */
  @Override
  public HtmlContext getHtmlContext() {
    return checkNotNull(
        htmlContext, "Cannot access HtmlContext before HtmlContextVisitor or InferenceEngine.");
  }

  public void setHtmlContext(HtmlContext value) {
    this.htmlContext = value;
  }

  public boolean isPassingData() {
    return isPassingAllData || getDataExpr() != null;
  }

  public void setTemplateCallKey(String key) {
    this.callKey = key;
  }

  public String getTemplateCallKey() {
    return callKey;
  }

  public boolean isPassingAllData() {
    return isPassingAllData;
  }

  public boolean isSelfClosing() {
    return this.selfClosing;
  }

  public boolean isErrorFallbackSkip() {
    return errorFallbackSkip;
  }

  @Override
  public List<CommandTagAttribute> getAttributes() {
    return attributes;
  }

  @Nullable
  public ExprRootNode getDataExpr() {
    return attributes.stream()
        .filter(a -> a.hasName(DATA) && a.hasExprValue())
        .findFirst()
        .map(a -> a.valueAsExprList().get(0))
        .orElse(null);
  }

  @Nullable
  public ExprRootNode getKeyExpr() {
    return attributes.stream()
        .filter(a -> a.hasName(KEY))
        .findFirst()
        .map(a -> a.valueAsExprList().get(0))
        .orElse(null);
  }

  public void setKeyExpr(ExprRootNode expr) {
    CommandTagAttribute existing =
        attributes.stream().filter(a -> a.hasName(KEY)).findFirst().orElse(null);
    if (existing == null && expr == null) {
      return;
    }

    List<CommandTagAttribute> newAttr = new ArrayList<>(attributes);
    if (existing != null) {
      newAttr.remove(existing);
      if (expr != null) {
        newAttr.add(
            new CommandTagAttribute(
                existing.getName(),
                existing.getQuoteStyle(),
                ImmutableList.of(expr.getRoot()),
                existing.getSourceLocation()));
      }
    } else {
      newAttr.add(
          new CommandTagAttribute(
              Identifier.create(KEY, UNKNOWN),
              QuoteStyle.DOUBLE,
              ImmutableList.of(expr.getRoot()),
              UNKNOWN));
    }
    attributes = ImmutableList.copyOf(newAttr);
  }

  public boolean getIsPcData() {
    return isPcData;
  }

  public void setIsPcData(boolean isPcData) {
    this.isPcData = isPcData;
  }

  @Override
  public MessagePlaceholder getPlaceholder() {
    return placeholder;
  }

  @Override
  public SamenessKey genSamenessKey() {
    // CallNodes are never considered the same placeholder. We return the node instance as the info
    // for determining sameness. Since nodes have identity semantics this will only compare equal
    // to itself.
    return new IdentitySamenessKey(this);
  }

  @Override
  public String getTagString() {
    return getTagString(numChildren() == 0); // tag is self-ending if it has no children
  }

  @Override
  public String toSourceString() {
    return (numChildren() == 0) ? getTagString() : super.toSourceString();
  }

  @Override
  public ImmutableList<ExprRootNode> getExprList() {
    return attributes.stream()
        .filter(CommandTagAttribute::hasExprValue)
        .flatMap(a -> a.valueAsExprList().stream())
        .collect(toImmutableList());
  }

  @SuppressWarnings("unchecked")
  @Override
  public ParentSoyNode<StandaloneNode> getParent() {
    return (ParentSoyNode<StandaloneNode>) super.getParent();
  }

  @Override
  public SourceLocation getOpenTagLocation() {
    return openTagLocation;
  }

  /** Returns the location of the callee name in the source code. */
  public abstract SourceLocation getSourceCalleeLocation();

  /**
   * Returns the escaping directives, applied from left to right.
   *
   * <p>It is an error to call this before the contextual rewriter has been run.
   */
  public ImmutableList<SoyPrintDirective> getEscapingDirectives() {
    return escapingDirectives;
  }

  /** Sets the inferred escaping directives. */
  public void setEscapingDirectives(ImmutableList<SoyPrintDirective> escapingDirectives) {
    this.escapingDirectives = escapingDirectives;
  }
}
