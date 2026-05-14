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

package com.google.template.soy.types;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.GenericDescriptor;
import com.google.template.soy.base.internal.Identifier;
import java.util.function.Function;
import javax.annotation.Nullable;

/** Registry of types which can be looked up by name. */
public interface SoyTypeRegistry extends TypeRegistry {

  /**
   * A type registry that defaults all unknown types to the 'unknown' type.
   *
   * @deprecated Do not use. Either avoid running passes that depend on type information or supply a
   *     proper type registry.
   */
  @Deprecated
  static SoyTypeRegistry defaultUnknown() {
    return new DelegatingSoyTypeRegistry(SoyTypeRegistryBuilder.create()) {
      @Nullable
      @Override
      public SoyType getType(String typeName) {
        SoyType type = super.getType(typeName);
        return type != null ? type : UnknownType.getInstance();
      }

      @Override
      public ProtoTypeRegistry getProtoRegistry() {
        return (fqn) -> UnknownType.getInstance();
      }
    };
  }

  /** Returns the list of proto file descriptors with which this registry was created. */
  default ImmutableSet<FileDescriptor> getProtoDescriptors() {
    return ImmutableSet.of();
  }

  /** Resolves a local proto symbol to a FQN. Returns null if no match is found. */
  @Nullable
  default Identifier resolve(Identifier id) {
    return null;
  }

  default ProtoTypeRegistry getProtoRegistry() {
    return (fqn) -> null;
  }

  SoyProtoType getOrComputeProtoType(
      Descriptor descriptor, Function<? super String, ? extends SoyProtoType> mapper);

  ImportType getProtoImportType(GenericDescriptor descriptor);

  SoyType getProtoImportType(FileDescriptor descriptor, String member);

  SoyType getProtoImportType(Descriptor descriptor, String member);

  default SoyType getOrCreateNamedType(String name, String namespace) {
    throw new UnsupportedOperationException();
  }
}
