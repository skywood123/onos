"""
 Copyright 2018-present Open Networking Foundation

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
"""

load("//tools/build/bazel:generate_workspace.bzl", "COMPILE", "TEST", "maven_coordinates")
load("//tools/build/bazel:variables.bzl", "ONOS_GROUP_ID", "ONOS_VERSION")

def dump(obj):
    for attr in dir(obj):
        print("obj.%s = %r" % (attr, getattr(obj, attr)))

# Implementation of a rule to produce an OSGi feature XML snippet
def _osgi_feature_impl(ctx):
    output = ctx.outputs.feature_xml

    args = [
        "-O",
        output.path,
        "-n",
        ctx.attr.name,
        "-v",
        ctx.attr.version,
        "-t",
        ctx.attr.description,
    ]

    inputs = []
    for dep in ctx.attr.included_bundles:
        args += ["-b", maven_coordinates(dep.label)]
        for f in dep.java.outputs.jars:
            inputs += [f.class_jar]

    for dep in ctx.attr.excluded_bundles:
        args += ["-e", maven_coordinates(dep.label)]
        for f in dep.java.outputs.jars:
            inputs += [f.class_jar]

    for f in ctx.attr.required_features:
        args += ["-f", f]

    args += ["-F" if ctx.attr.generate_file else "-E"]

    ctx.actions.run(
        inputs = inputs,
        outputs = [output],
        arguments = args,
        progress_message = "Generating feature %s" % ctx.attr.name,
        executable = ctx.executable._writer,
    )

osgi_feature = rule(
    attrs = {
        "description": attr.string(),
        "version": attr.string(default = ONOS_VERSION),
        "required_features": attr.string_list(default = ["onos-api"]),
        "included_bundles": attr.label_list(),
        "excluded_bundles": attr.label_list(default = []),
        "generate_file": attr.bool(default = False),
        "_writer": attr.label(
            executable = True,
            cfg = "host",
            allow_files = True,
            default = Label("//tools/build/bazel:onos_app_writer"),
        ),
    },
    outputs = {
        "feature_xml": "feature-%{name}.xml",
    },
    implementation = _osgi_feature_impl,
)

# OSGi feature XML header & footer constants
FEATURES_HEADER = '''\
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<features xmlns="http://karaf.apache.org/xmlns/features/v1.2.0"
          name="onos-%s">
    <repository>mvn:org.apache.karaf.features/standard/3.0.8/xml/features</repository>
''' % ONOS_VERSION

FEATURES_FOOTER = "</features>"

# Implementation of a rule to produce an OSGi feature repo XML file
def _osgi_feature_repo_impl(ctx):
    output = ctx.outputs.feature_repo_xml

    cmd = "(echo '%s';" % FEATURES_HEADER
    inputs = []
    for dep in ctx.attr.exported_features:
        for f in dep.files.to_list():
            inputs += [f]
            cmd += "cat %s;" % f.path
    cmd += "echo '%s') > %s;" % (FEATURES_FOOTER, output.path)

    ctx.actions.run_shell(
        inputs = inputs,
        outputs = [output],
        progress_message = "Generating feature repo %s" % ctx.attr.name,
        command = cmd,
    )

osgi_feature_repo = rule(
    attrs = {
        "description": attr.string(),
        "version": attr.string(default = ONOS_VERSION),
        "exported_features": attr.label_list(),
    },
    outputs = {
        "feature_repo_xml": "feature-repo-%{name}.xml",
    },
    implementation = _osgi_feature_repo_impl,
)
