/*
 * Copyright 2019 Google Inc.
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

package com.google.template.soy.shared.internal.gencode;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.base.Ascii;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.GenericDescriptor;
import com.google.template.soy.base.SourceLogicalPath;
import com.google.template.soy.base.internal.SoyFileKind;
import com.google.template.soy.exprtree.FunctionNode;
import com.google.template.soy.exprtree.MethodCallNode;
import com.google.template.soy.shared.internal.BuiltinFunction;
import com.google.template.soy.shared.internal.BuiltinMethod;
import com.google.template.soy.soytree.ImportNode.ImportType;
import com.google.template.soy.soytree.SoyFileNode;
import com.google.template.soy.soytree.SoyTreeUtils;
import com.google.template.soy.types.ProtoExtensionImportType;
import com.google.template.soy.types.SoyProtoEnumType;
import com.google.template.soy.types.SoyProtoType;
import com.google.template.soy.types.SoyType;
import com.google.template.soy.types.SoyTypeRegistry;
import com.google.template.soy.types.SoyTypes;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Utils for writing generated Java classes. */
public final class JavaGenerationUtils {

  /** Pattern for an all-upper-case word in a file name or identifier. */
  private static final Pattern ALL_UPPER_WORD =
      Pattern.compile("(?<= [^A-Za-z] | ^)  [A-Z]+  (?= [^A-Za-z] | $)", Pattern.COMMENTS);

  /** Pattern for an all-lower-case word in a file name or identifier. */
  // Note: Char after an all-lower word can be an upper letter (e.g. first word of camel case).
  private static final Pattern ALL_LOWER_WORD =
      Pattern.compile("(?<= [^A-Za-z] | ^)  [a-z]+  (?= [^a-z] | $)", Pattern.COMMENTS);

  /** Pattern for a character that's not a letter nor a digit. */
  private static final Pattern NON_LETTER_DIGIT = Pattern.compile("[^A-Za-z0-9]");

  // See https://docs.oracle.com/javase/tutorial/java/nutsandbolts/_keywords.html
  private static final ImmutableSet<String> RESERVED_JAVA_KEYWORDS =
      ImmutableSet.of(
          "abstract",
          "assert",
          "boolean",
          "break",
          "byte",
          "case",
          "catch",
          "char",
          "class",
          "const",
          "continue",
          "default",
          "do",
          "double",
          "else",
          "enum",
          "extends",
          "false",
          "final",
          "finally",
          "float",
          "for",
          "goto",
          "if",
          "implements",
          "import",
          "instanceof",
          "int",
          "interface",
          "long",
          "native",
          "new",
          "null",
          "package",
          "private",
          "protected",
          "public",
          "return",
          "short",
          "static",
          "strictfp",
          "super",
          "switch",
          "synchronized",
          "this",
          "throw",
          "throws",
          "transient",
          "true",
          "try",
          "void",
          "volatile",
          "while");

  private JavaGenerationUtils() {}

  /**
   * Formats and appends a Javadoc comment to the code being built.
   *
   * @param ilb The builder for the code.
   * @param doc The doc string to append as the content of a Javadoc comment. The Javadoc format
   *     will follow the usual conventions. Important: If the doc string is multiple lines, the line
   *     separator must be '\n'.
   * @param forceMultiline If true, we always generate a multiline Javadoc comment even if the doc
   *     string only has one line. If false, we generate either a single line or multiline Javadoc
   *     comment, depending on the doc string.
   * @param wrapAt100Chars If true, wrap at 100 chars.
   */
  public static void appendJavadoc(
      IndentedLinesBuilder ilb, String doc, boolean forceMultiline, boolean wrapAt100Chars) {
    if (wrapAt100Chars) {
      // Actual wrap length is less because of indent and because of space used by Javadoc chars.
      int wrapLen = 100 - ilb.getCurrIndentLen() - 7;
      List<String> wrappedLines = Lists.newArrayList();
      for (String line : Splitter.on('\n').split(doc)) {
        while (line.length() > wrapLen) {
          int spaceIndex = line.lastIndexOf(' ', wrapLen);
          if (spaceIndex >= 0) {
            wrappedLines.add(line.substring(0, spaceIndex));
            line = line.substring(spaceIndex + 1); // add 1 to skip the space
          } else {
            // No spaces. Just wrap at wrapLen.
            wrappedLines.add(line.substring(0, wrapLen));
            line = line.substring(wrapLen);
          }
        }
        wrappedLines.add(line);
      }
      doc = Joiner.on("\n").join(wrappedLines);
    }

    if (doc.contains("\n") || forceMultiline) {
      // Multiline.
      ilb.appendLine("/**");
      for (String line : Splitter.on('\n').split(doc)) {
        ilb.appendLineStart(" *");
        if (!line.isEmpty()) {
          ilb.appendLineMiddle(" ", line);
        }
        ilb.appendLineEnd();
      }
      ilb.appendLine(" */");

    } else {
      // One line.
      ilb.appendLine("/** ", doc, " */");
    }
  }

  /**
   * Creates the upper camel case version of the given string, stripping non-alphanumberic
   * characters.
   *
   * @param str The string to turn into upper camel case.
   * @return The upper camel case version of the string.
   */
  public static String makeUpperCamelCase(String str) {
    str = makeWordsCapitalized(str, ALL_UPPER_WORD);
    str = makeWordsCapitalized(str, ALL_LOWER_WORD);
    str = NON_LETTER_DIGIT.matcher(str).replaceAll("");
    return str;
  }

  public static String makeLowerCamelCase(String str) {
    str = makeUpperCamelCase(str);
    return Character.toLowerCase(str.charAt(0)) + str.substring(1);
  }

  /**
   * Makes all the words in the given string into capitalized format (first letter capital, rest
   * lower case). Words are defined by the given regex pattern.
   *
   * @param str The string to process.
   * @param wordPattern The regex pattern for matching a word.
   * @return The resulting string with all words in capitalized format.
   */
  private static String makeWordsCapitalized(String str, Pattern wordPattern) {
    StringBuffer sb = new StringBuffer();

    Matcher wordMatcher = wordPattern.matcher(str);
    while (wordMatcher.find()) {
      String oldWord = wordMatcher.group();
      StringBuilder newWord = new StringBuilder();
      for (int i = 0, n = oldWord.length(); i < n; i++) {
        if (i == 0) {
          newWord.append(Character.toUpperCase(oldWord.charAt(i)));
        } else {
          newWord.append(Character.toLowerCase(oldWord.charAt(i)));
        }
      }
      wordMatcher.appendReplacement(sb, Matcher.quoteReplacement(newWord.toString()));
    }
    wordMatcher.appendTail(sb);

    return sb.toString();
  }

  /**
   * Private helper to append an ImmutableList to the code, on a new line.
   *
   * @param ilb The builder for the code.
   * @param typeParamSnippet The type parameter for the ImmutableList.
   * @param itemSnippets Code snippets for the items to put into the ImmutableList.
   */
  public static void appendImmutableList(
      IndentedLinesBuilder ilb, String typeParamSnippet, Collection<String> itemSnippets) {
    ilb.appendLineStart("");
    appendImmutableListInline(ilb, typeParamSnippet, itemSnippets);
  }

  /**
   * Private helper to append an ImmutableList to the code, inline.
   *
   * @param ilb The builder for the code.
   * @param typeParamSnippet The type parameter for the ImmutableList.
   * @param itemSnippets Code snippets for the items to put into the ImmutableList.
   */
  public static void appendImmutableListInline(
      IndentedLinesBuilder ilb, String typeParamSnippet, Collection<String> itemSnippets) {
    appendFunctionCallWithParamsOnNewLines(
        ilb, "com.google.common.collect.ImmutableList." + typeParamSnippet + "of", itemSnippets);
  }

  /**
   * Private helper to append an ImmutableSet to the code, on a new line.
   *
   * @param ilb The builder for the code.
   * @param typeParamSnippet The type parameter for the ImmutableList.
   * @param itemSnippets Code snippets for the items to put into the ImmutableList.
   */
  public static void appendImmutableSet(
      IndentedLinesBuilder ilb, String typeParamSnippet, Collection<String> itemSnippets) {
    ilb.appendLineStart("");
    appendImmutableSetInline(ilb, typeParamSnippet, itemSnippets);
  }

  /**
   * Private helper to append an ImmutableSet to the code, inline.
   *
   * @param ilb The builder for the code.
   * @param typeParamSnippet The type parameter for the ImmutableList.
   * @param itemSnippets Code snippets for the items to put into the ImmutableList.
   */
  public static void appendImmutableSetInline(
      IndentedLinesBuilder ilb, String typeParamSnippet, Collection<String> itemSnippets) {
    appendFunctionCallWithParamsOnNewLines(
        ilb, "ImmutableSet." + typeParamSnippet + "of", itemSnippets);
  }

  /**
   * Private helper to append an ImmutableMap to the code.
   *
   * @param ilb The builder for the code.
   * @param typeParamSnippet The type parameter for the ImmutableMap.
   * @param entrySnippetPairs Pairs of (key, value) code snippets for the entries to put into the
   *     ImmutableMap.
   */
  public static void appendImmutableMap(
      IndentedLinesBuilder ilb, String typeParamSnippet, Map<String, String> entrySnippetPairs) {
    if (entrySnippetPairs.isEmpty()) {
      ilb.appendLineStart("com.google.common.collect.ImmutableMap.", typeParamSnippet, "of()");

    } else {
      ilb.appendLine("com.google.common.collect.ImmutableMap.", typeParamSnippet, "builder()");
      ilb.increaseIndent(2);
      for (Map.Entry<String, String> entrySnippetPair : entrySnippetPairs.entrySet()) {
        ilb.appendLine(".put(", entrySnippetPair.getKey(), ", ", entrySnippetPair.getValue(), ")");
      }
      ilb.appendLineStart(".build()");
      ilb.decreaseIndent(2);
    }
  }

  /**
   * Private helper for appendImmutableList() and appendImmutableSet().
   *
   * @param ilb The builder for the code.
   * @param functionCallSnippet Code snippet for the function call (without parenthesis or params).
   * @param params Params to put in parenthesis for the function call.
   */
  public static void appendFunctionCallWithParamsOnNewLines(
      IndentedLinesBuilder ilb, String functionCallSnippet, Collection<String> params) {

    if (params.isEmpty()) {
      ilb.appendParts(functionCallSnippet, "()");
      return;
    }

    ilb.appendLineEnd(functionCallSnippet, "(");
    ilb.increaseIndent(2); // Double indent each param.
    boolean isFirst = true;
    for (String param : params) {
      if (isFirst) {
        isFirst = false;
      } else {
        ilb.appendLineEnd(",");
      }
      ilb.appendLineStart(param);
    }
    ilb.appendLineMiddle(")");
    ilb.decreaseIndent(2);
  }

  public static ImmutableSet<GenericDescriptor> getProtoTypes(
      SoyFileNode node, SoyTypeRegistry typeRegistry) {
    // Get any enums or messages from imports. Extensions are handled by the global pass.
    Stream<GenericDescriptor> fromImports =
        node.getImports().stream()
            .filter(i -> i.getImportType() == ImportType.PROTO)
            .flatMap(i -> i.getIdentifiers().stream())
            .map(varName -> typeRegistry.getType(varName.name()))
            .filter(Objects::nonNull)
            .map(
                type -> {
                  if (type instanceof SoyProtoType) {
                    return ((SoyProtoType) type).getDescriptor();
                  } else if (type instanceof SoyProtoEnumType) {
                    return ((SoyProtoEnumType) type).getDescriptor();
                  } else if (type instanceof ProtoExtensionImportType) {
                    return ((ProtoExtensionImportType) type).getDescriptor();
                  }
                  return null;
                })
            .filter(Objects::nonNull);

    // Collect the following:
    // + for any params whose type is a proto, get the proto name and Java class name.
    Stream<GenericDescriptor> fromHeader =
        node.getTemplates().stream()
            .flatMap(t -> t.getHeaderParams().stream())
            .flatMap(varDefn -> findProtoTypes(varDefn.type(), typeRegistry));

    // anything else that may have a type now or in the future.

    // Add references for return types of getExtension method.
    Stream<GenericDescriptor> fromCall =
        SoyTreeUtils.allNodesOfType(node, MethodCallNode.class)
            .filter(MethodCallNode::isMethodResolved)
            .filter(n -> n.getSoyMethod() instanceof BuiltinMethod)
            .flatMap(
                methodNode ->
                    ((BuiltinMethod) methodNode.getSoyMethod())
                        .getProtoDependencyTypes(methodNode).stream());

    // Add proto init
    Stream<GenericDescriptor> fromProtoInit =
        SoyTreeUtils.allNodesOfType(node, FunctionNode.class)
            .filter(fctNode -> fctNode.getSoyFunction() == BuiltinFunction.PROTO_INIT)
            .filter(fctNode -> fctNode.getType() instanceof SoyProtoType)
            .flatMap(
                fctNode -> {
                  SoyProtoType proto = (SoyProtoType) fctNode.getType();
                  return Streams.concat(
                      Stream.of(proto.getDescriptor()),
                      fctNode.getParamNames().stream()
                          .map(paramName -> proto.getFieldDescriptor(paramName.identifier()))
                          .filter(FieldDescriptor::isExtension));
                });

    return Streams.concat(fromImports, fromHeader, fromCall, fromProtoInit)
        .distinct()
        .filter(
            d ->
                typeRegistry
                        .getProtoRegistry()
                        .getDepKind(SourceLogicalPath.create(d.getFile().getFullName()))
                    == SoyFileKind.DEP)
        .collect(toImmutableSet());
  }

  /** Recursively search for protocol buffer types within the given type. */
  private static Stream<GenericDescriptor> findProtoTypes(
      SoyType root, SoyTypeRegistry typeRegistry) {
    return SoyTypes.allConcreteTypes(root, typeRegistry)
        .map(
            type -> {
              switch (type.getKind()) {
                case PROTO:
                  return ((SoyProtoType) type).getDescriptor();
                case PROTO_ENUM:
                  return ((SoyProtoEnumType) type).getDescriptor();
                default:
                  return null;
              }
            })
        .filter(Objects::nonNull);
  }

  /** Returns whether the given symbol is a keyword reserved by the Java language. */
  public static boolean isReservedKeyword(String symbol) {
    return RESERVED_JAVA_KEYWORDS.contains(symbol);
  }

  /**
   * Returns the simple class name of the outer class of the per-soy file invocations builders, e.g.
   * file_name.soy -> FileNameTemplates`.
   *
   * @param fileName the file name only, not including directory path.
   */
  public static String buildTemplatesClassName(String fileName) {
    Preconditions.checkNotNull(fileName);
    Preconditions.checkArgument(!fileName.contains("/") && !fileName.contains("\\"));
    if (Ascii.toLowerCase(fileName).endsWith(".soy")) {
      fileName = fileName.substring(0, fileName.length() - 4);
    }
    String prefix = makeUpperCamelCase(fileName);
    if (Character.isDigit(prefix.charAt(0))) {
      prefix = "_" + prefix;
    }
    return prefix + "Templates";
  }

  /**
   * Returns the simple class name of the Builder class for FQN (or local name) template name {@code
   * namespacedTemplateName}. This computed value is simply the upper camel case form of the local
   * template name, i.e. ns.foo -> Foo.
   */
  public static String buildTemplateClassName(String namespacedTemplateName) {
    String templateName =
        namespacedTemplateName.substring(namespacedTemplateName.lastIndexOf('.') + 1);

    // Convert the template name to upper camel case (stripping non-alphanumeric characters),  (e.g.
    // template "foo" -> "Foo").
    return makeUpperCamelCase(templateName);
  }
}
