# Copyright 2013 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# NOTE: THIS IS A WORK IN PROGRESS AND IS NOT EXPECTED TO WORK AS WE HAVE
# NO CONTINUOUS INTEGRATION.

workspace(name = "com_google_closure_templates")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

RULES_JVM_EXTERNAL_TAG = "3.0"

RULES_JVM_EXTERNAL_SHA = "62133c125bf4109dfd9d2af64830208356ce4ef8b165a6ef15bbff7460b35c3a"

http_archive(
    name = "rules_jvm_external",
    sha256 = RULES_JVM_EXTERNAL_SHA,
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    urls = [
        "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG,
    ],
)

load("@rules_jvm_external//:defs.bzl", "maven_install")
load("@rules_jvm_external//:specs.bzl", "maven")

# Keep in sync with values in `pom.xml`.
MAVEN_PROPERTIES = {
    "args4j.version": "2.0.23",
    "asm.version": "7.0",
    "autovalue.version": "1.6.3",
    "errorprone.version": "2.1.3",
    "findbugsjsr305.version": "3.0.2",
    "gson.version": "2.7",
    "guava.version": "25.1-jre",
    "guice.version": "4.1.0",
    "html_types.version": "1.0.6",
    "icu4j.version": "57.1",
    "javaxinject.version": "1",
    "json.version": "20160212",
    "jsr250.version": "1.0",
    "junit.version": "4.13-beta-1",
    "proto.version": "3.3.0",
    "rhino.version": "1.7R3",
    "truth.version": "0.45",
}

maven_install(
    name = "maven",
    artifacts = [
        maven.artifact(
            group = "javax.inject",
            artifact = "javax.inject",
            version = MAVEN_PROPERTIES["javaxinject.version"],
        ),
        maven.artifact(
            group = "com.google.inject",
            artifact = "guice",
            version = MAVEN_PROPERTIES["guice.version"],
            exclusions = [
                # still depends on guava 19. exclude it.
                maven.exclusion(
                    group = "com.google.guava",
                    artifact = "guava",
                ),
                maven.exclusion(
                    group = "com.google.guava",
                    artifact = "guava-testlib",
                ),
            ],
        ),
        maven.artifact(
            group = "com.google.inject.extensions",
            artifact = "guice-multibindings",
            version = MAVEN_PROPERTIES["guice.version"],
        ),
        maven.artifact(
            group = "com.google.guava",
            artifact = "guava",
            version = MAVEN_PROPERTIES["guava.version"],
        ),
        maven.artifact(
            group = "com.google.guava",
            artifact = "guava-testlib",
            version = MAVEN_PROPERTIES["guava.version"],
            exclusions = [
                maven.exclusion(
                    group = "junit",
                    artifact = "junit",
                ),
                maven.exclusion(
                    group = "com.google.code.findbugs",
                    artifact = "jsr305",
                ),
                maven.exclusion(
                    group = "com.google.errorprone",
                    artifact = "error_prone_annotations",
                ),
                maven.exclusion(
                    group = "org.checkerframework",
                    artifact = "checker-qual",
                ),
            ],
            # testonly = True,  # <scope>test</scope>
        ),
        maven.artifact(
            group = "com.google.auto.value",
            artifact = "auto-value",
            version = MAVEN_PROPERTIES["autovalue.version"],
            exclusions = [
                maven.exclusion(
                    group = "com.google.guava",
                    artifact = "guava",
                ),
                maven.exclusion(
                    group = "org.ow2.asm",
                    artifact = "asm",
                ),
            ],
            neverlink = True,  # <scope>provided</scope>
        ),
        maven.artifact(
            group = "com.google.auto.value",
            artifact = "auto-value-annotations",
            version = MAVEN_PROPERTIES["autovalue.version"],
            neverlink = True,  # <scope>provided</scope>
        ),
        maven.artifact(
            group = "com.google.common.html.types",
            artifact = "types",
            version = MAVEN_PROPERTIES["html_types.version"],
            exclusions = [
                maven.exclusion(
                    group = "com.google.errorprone",
                    artifact = "error_prone_annotations",
                ),
                # still depends on guava 19. exclude it.
                maven.exclusion(
                    group = "com.google.guava",
                    artifact = "guava",
                ),
                maven.exclusion(
                    group = "com.google.guava",
                    artifact = "guava-testlib",
                ),
                maven.exclusion(
                    group = "com.google.protobuf",
                    artifact = "protobuf-java",
                ),
            ],
        ),
        maven.artifact(
            group = "com.google.errorprone",
            artifact = "error_prone_annotations",
            version = MAVEN_PROPERTIES["errorprone.version"],
        ),
        maven.artifact(
            group = "com.google.protobuf",
            artifact = "protobuf-java",
            version = MAVEN_PROPERTIES["proto.version"],
        ),
        maven.artifact(
            group = "org.ow2.asm",
            artifact = "asm",
            version = MAVEN_PROPERTIES["asm.version"],
        ),
        maven.artifact(
            group = "org.ow2.asm",
            artifact = "asm-commons",
            version = MAVEN_PROPERTIES["asm.version"],
        ),
        maven.artifact(
            group = "org.ow2.asm",
            artifact = "asm-util",
            version = MAVEN_PROPERTIES["asm.version"],
        ),
        maven.artifact(
            group = "org.ow2.asm",
            artifact = "asm-analysis",
            version = MAVEN_PROPERTIES["asm.version"],
        ),
        maven.artifact(
            group = "org.ow2.asm",
            artifact = "asm-tree",
            version = MAVEN_PROPERTIES["asm.version"],
        ),
        maven.artifact(
            group = "com.google.truth",
            artifact = "truth",
            version = MAVEN_PROPERTIES["truth.version"],
            exclusions = [
                maven.exclusion(
                    group = "com.google.guava",
                    artifact = "guava",
                ),
                maven.exclusion(
                    group = "com.google.errorprone",
                    artifact = "error_prone_annotations",
                ),
                maven.exclusion(
                    group = "junit",
                    artifact = "junit",
                ),
            ],
            # testonly = True,  # <scope>test</scope>
        ),
        maven.artifact(
            group = "com.google.truth.extensions",
            artifact = "truth-java8-extension",
            version = MAVEN_PROPERTIES["truth.version"],
            exclusions = [
                maven.exclusion(
                    group = "com.google.guava",
                    artifact = "guava",
                ),
                maven.exclusion(
                    group = "com.google.errorprone",
                    artifact = "error_prone_annotations",
                ),
                maven.exclusion(
                    group = "junit",
                    artifact = "junit",
                ),
            ],
            # testonly = True,  # <scope>test</scope>
        ),
        maven.artifact(
            group = "com.ibm.icu",
            artifact = "icu4j",
            version = MAVEN_PROPERTIES["icu4j.version"],
        ),
        maven.artifact(
            group = "args4j",
            artifact = "args4j",
            version = MAVEN_PROPERTIES["args4j.version"],
        ),
        maven.artifact(
            group = "args4j",
            artifact = "args4j",
            version = MAVEN_PROPERTIES["args4j.version"],
        ),
        maven.artifact(
            group = "com.google.code.findbugs",
            artifact = "jsr305",
            version = MAVEN_PROPERTIES["findbugsjsr305.version"],
        ),
        maven.artifact(
            group = "com.google.code.gson",
            artifact = "gson",
            version = MAVEN_PROPERTIES["gson.version"],
        ),
        maven.artifact(
            group = "org.json",
            artifact = "json",
            version = MAVEN_PROPERTIES["json.version"],
        ),
        maven.artifact(
            group = "javax.annotation",
            artifact = "jsr250-api",
            version = MAVEN_PROPERTIES["jsr250.version"],
        ),
        maven.artifact(
            group = "org.mozilla",
            artifact = "rhino",
            version = MAVEN_PROPERTIES["rhino.version"],
            # testonly = True,  # <scope>test</scope>
        ),
        maven.artifact(
            group = "junit",
            artifact = "junit",
            version = MAVEN_PROPERTIES["junit.version"],
            # testonly = True,  # <scope>test</scope>
        ),
    ],
    fetch_sources = True,  # Fetch source jars. Defaults to False.
    override_targets = {
        # TODO(yannic): Figure out how to use `java_plugin`.
        "com.google.auto.value:auto-value": "@com_google_auto_value//:processor",

        # `java_proto_library` has an implicit dependency on `@com_google_protobuf//:protobuf_java`.
        "com.google.protobuf:protobuf-java": "@com_google_protobuf//:protobuf_java",
    },
    repositories = [
        "https://maven.google.com",
        "https://repo1.maven.org/maven2",
    ],
    strict_visibility = True,
)

load("@bazel_tools//tools/build_defs/repo:java.bzl", "java_import_external")

# Apache 2.0
#
# AutoValue 1.6+ shades Guava, Auto Common, and JavaPoet. That's OK
# because none of these jars become runtime dependencies.
java_import_external(
    name = "com_google_auto_value",
    default_visibility = ["//visibility:private"],
    extra_build_file_content = """
java_plugin(
    name = "AutoAnnotationProcessor",
    output_licenses = ["unencumbered"],
    processor_class = "com.google.auto.value.processor.AutoAnnotationProcessor",
    tags = ["annotation=com.google.auto.value.AutoAnnotation;genclass=${package}.AutoAnnotation_${outerclasses}${classname}_${methodname}"],
    deps = [":lib"],
)

java_plugin(
    name = "AutoOneOfProcessor",
    output_licenses = ["unencumbered"],
    processor_class = "com.google.auto.value.processor.AutoOneOfProcessor",
    tags = ["annotation=com.google.auto.value.AutoValue;genclass=${package}.AutoOneOf_${outerclasses}${classname}"],
    deps = [":lib"],
)

java_plugin(
    name = "AutoValueProcessor",
    output_licenses = ["unencumbered"],
    processor_class = "com.google.auto.value.processor.AutoValueProcessor",
    tags = ["annotation=com.google.auto.value.AutoValue;genclass=${package}.AutoValue_${outerclasses}${classname}"],
    deps = [":lib"],
)

java_library(
    name = "processor",
    exported_plugins = [
        ":AutoAnnotationProcessor",
        ":AutoOneOfProcessor",
        ":AutoValueProcessor",
    ],
    exports = [
        "@maven//:com_google_auto_value_auto_value_annotations",
    ],
    visibility = [
        "@maven//:__subpackages__",
    ],
)
""",
    generated_rule_name = "lib",
    jar_sha256 = "ed5f69ef035b5367f1f0264f843b988908e36e155845880b29d79b7c8855adf3",
    jar_urls = [
        "https://repo1.maven.org/maven2/com/google/auto/value/auto-value/1.6.3/auto-value-1.6.3.jar",
    ],
)

# Apache 2.0
http_archive(
    name = "rules_java",
    sha256 = "52423cb07384572ab60ef1132b0c7ded3a25c421036176c0273873ec82f5d2b2",
    urls = [
        "https://github.com/bazelbuild/rules_java/releases/download/0.1.0/rules_java-0.1.0.tar.gz",
    ],
)

load("@rules_java//java:repositories.bzl", "rules_java_dependencies", "rules_java_toolchains")

rules_java_dependencies()

rules_java_toolchains()

# Apache 2.0
http_archive(
    name = "rules_proto",
    sha256 = "57001a3b33ec690a175cdf0698243431ef27233017b9bed23f96d44b9c98242f",
    strip_prefix = "rules_proto-9cd4f8f1ede19d81c6d48910429fe96776e567b1",
    urls = [
        "https://github.com/bazelbuild/rules_proto/archive/9cd4f8f1ede19d81c6d48910429fe96776e567b1.tar.gz",
    ],
)

load("@rules_proto//proto:repositories.bzl", "rules_proto_dependencies", "rules_proto_toolchains")

rules_proto_dependencies()

rules_proto_toolchains()

# Apache 2.0
http_archive(
    name = "xyz_yannic_rules_javacc",
    sha256 = "737471214ad1e95398dab51659c75e0016ec6a2e9b93317b1a20753ce9d3e91f",
    strip_prefix = "xyz_yannic_rules_javacc-2c5682e28ce9ff92b672f6bfcea244d627289ca0",
    urls = [
        "https://github.com/bazel-packages/xyz_yannic_rules_javacc/archive/2c5682e28ce9ff92b672f6bfcea244d627289ca0.tar.gz",
    ],
)

load("@xyz_yannic_rules_javacc//javacc:repositories.bzl", "rules_javacc_dependencies", "rules_javacc_toolchains")

rules_javacc_dependencies()

rules_javacc_toolchains()
