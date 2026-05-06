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

package com.google.template.soy.passes;

import com.google.template.soy.base.SourceLocation;
import com.google.template.soy.base.SourceLocationMapper;
import com.google.template.soy.base.internal.IdGenerator;
import com.google.template.soy.shared.internal.Sanitizers;
import com.google.template.soy.soytree.AbstractSoyNodeVisitor;
import com.google.template.soy.soytree.HtmlAttributeNode;
import com.google.template.soy.soytree.HtmlAttributeValueNode;
import com.google.template.soy.soytree.HtmlOpenTagNode;
import com.google.template.soy.soytree.RawTextNode;
import com.google.template.soy.soytree.SoyFileNode;
import com.google.template.soy.soytree.SoyNode;
import com.google.template.soy.soytree.SoyNode.ParentSoyNode;

/** Adds a data-sourcecode-loc attribute to all HTML open tags. */
@RunBefore(
    // Annotations should happen before HTML desugaring and optimization.
    ResolveExpressionTypesPass.class)
final class AddHtmlSourceLocationsPass implements CompilerFilePass {

  public static final String DATA_SOURCECODE_LOC = "data-sourcecode-loc";

  public static final String FILE_PREFIX = "";

  @Override
  public void run(SoyFileNode file, IdGenerator nodeIdGen) {
    new Visitor(nodeIdGen, file.getSourceMap()).exec(file);
  }

  private static final class Visitor extends AbstractSoyNodeVisitor<Void> {
    private final IdGenerator nodeIdGen;
    private final SourceLocationMapper sourceMap;

    Visitor(IdGenerator nodeIdGen, SourceLocationMapper sourceMap) {
      this.nodeIdGen = nodeIdGen;
      this.sourceMap = sourceMap;
    }

    @Override
    protected void visitHtmlOpenTagNode(HtmlOpenTagNode node) {
      // We always want to visit our children.
      visitChildren(node);
      // Skip nodes that are not static HTML or already have a source location, e.g. because of
      // a build step that produced a soy file, and wants to provide the location of *its* original
      // source file.
      // Could we support element composition calls here? Might be worth looking into.
      if (!node.getTagName().isStatic()
          // <parameter> elements are used to pass arguments to template/element calls, not actual
          // HTML. See:
          // https://github.com/google/closure-templates/blob/master/documentation/reference/element-composition.md#pass-html-parameters-as-parameter-param
          || node.getTagName().getStaticTagName().equals("parameter")
          || node.getDirectAttributeNamed(DATA_SOURCECODE_LOC) != null) {
        return;
      }
      SourceLocation originalLoc = sourceMap.map(node.getSourceLocation());
      if (!node.getTaggedPairs().isEmpty()) {
        originalLoc =
            originalLoc.extend(sourceMap.map(node.getTaggedPairs().get(0).getSourceLocation()));
      }
      if (!originalLoc.isKnown()) {
        return;
      }
      // The TSX source maps don't map perfectly.
      // They start opening tags at after the opening angle bracket,
      // and end them at the closing angle bracket, so we need to use a corrected original source
      // location to account for this.
      if (originalLoc.getFilePath().path().endsWith(".tsx")) {
        originalLoc =
            new SourceLocation(
                originalLoc.getFilePath(),
                SourceLocation.Point.create(
                    originalLoc.getBeginLine(), Math.max(1, originalLoc.getBeginColumn() - 1)),
                SourceLocation.Point.create(
                    originalLoc.getEndLine(),
                    originalLoc.getEndColumn() + node.getTagName().getStaticTagName().length()));
      }
      SourceLocation currentLoc = node.getSourceLocation();
      HtmlAttributeNode attribute =
          new HtmlAttributeNode(nodeIdGen.genId(), currentLoc, currentLoc.getBeginPoint());
      attribute.addChild(new RawTextNode(nodeIdGen.genId(), DATA_SOURCECODE_LOC, currentLoc));
      HtmlAttributeValueNode attrValue =
          new HtmlAttributeValueNode(
              nodeIdGen.genId(), currentLoc, HtmlAttributeValueNode.Quotes.DOUBLE);
      attribute.addChild(attrValue);

      attrValue.addChild(
          new RawTextNode(
              nodeIdGen.genId(),
              Sanitizers.escapeHtmlAttribute(
                  String.format(
                      "%s%s;l=%d-%d;c=%d-%d",
                      FILE_PREFIX,
                      originalLoc.getFilePath().path(),
                      originalLoc.getBeginLine(),
                      originalLoc.getEndLine(),
                      originalLoc.getBeginColumn(),
                      originalLoc.getEndColumn() + 1)),
              currentLoc));

      node.addChild(attribute);
    }

    @Override
    protected void visitSoyNode(SoyNode node) {
      if (node instanceof ParentSoyNode<?> parentSoyNode) {
        visitChildren(parentSoyNode);
      }
    }
  }
}
