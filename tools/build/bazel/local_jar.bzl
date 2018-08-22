# Copyright 2018-present Open Networking Foundation
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

def _local_jar_impl(repository_ctx):
    repository_ctx.symlink(repository_ctx.attr.path, "jar/%s.jar" % repository_ctx.attr.name)
    repository_ctx.file("jar/BUILD", content = """
# DO NOT EDIT: automatically generated BUILD file for local_jar rule
java_import(
    name = "jar",
    jars = ["%s.jar"],
    visibility = ['//visibility:public']
)
    """ % repository_ctx.attr.name)

# Workspace rule to allow override of a single locally built 3rd party jar
local_jar = repository_rule(
    implementation = _local_jar_impl,
    local = True,
    attrs = {"path": attr.string(mandatory = True)},
)

# Macro to allow building ONOS against locally-built Atomix artifacts
def local_atomix(path, version):
    local_jar(
        name = "atomix",
        path = "%s/core/target/atomix-%s.jar" % (path, version),
    )
    local_jar(
        name = "atomix_cluster",
        path = "%s/cluster/target/atomix-cluster-%s.jar" % (path, version),
    )
    local_jar(
        name = "atomix_dist",
        path = "%s/dist/target/atomix-dist-%s.jar" % (path, version),
    )
    local_jar(
        name = "atomix_primitive",
        path = "%s/primitive/target/atomix-primitive-%s.jar" % (path, version),
    )
    local_jar(
        name = "atomix_tests",
        path = "%s/tests/target/atomix-tests-%s.jar" % (path, version),
    )
    local_jar(
        name = "atomix_utils",
        path = "%s/utils/target/atomix-utils-%s.jar" % (path, version),
    )
    local_jar(
        name = "atomix_agent",
        path = "%s/agent/target/atomix-agent-%s.jar" % (path, version),
    )
    local_jar(
        name = "atomix_storage",
        path = "%s/storage/target/atomix-storage-%s.jar" % (path, version),
    )
    local_jar(
        name = "atomix_gossip",
        path = "%s/protocols/gossip/target/atomix-gossip-%s.jar" % (path, version),
    )
    local_jar(
        name = "atomix_primary_backup",
        path = "%s/protocols/primary-backup/target/atomix-primary-backup-%s.jar" % (path, version),
    )
    local_jar(
        name = "atomix_raft",
        path = "%s/protocols/raft/target/atomix-raft-%s.jar" % (path, version),
    )
    local_jar(
        name = "atomix_rest",
        path = "%s/rest/target/atomix-rest-%s.jar" % (path, version),
    )

# TODO: add local_yang_tools, etc.
