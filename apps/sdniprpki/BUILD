COMPILE_DEPS = CORE_DEPS + JACKSON + CLI + [
    "//apps/routing-api:onos-apps-routing-api",
    "//apps/route-service/api:onos-apps-route-service-api",
    "//apps/intentsync:onos-apps-intentsync",
    "//apps/routing/common:onos-apps-routing-common",
    "@maven//:org_json_json_20190722",
]

BUNDLES = [
    "//apps/sdniprpki:onos-apps-sdniprpki",
    "//apps/routing-api:onos-apps-routing-api",
    "//apps/routing/common:onos-apps-routing-common",
    "@maven//:org_json_json_20190722",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//apps/routing-api:onos-apps-routing-api-tests",
    "//apps/route-service/api:onos-apps-route-service-api-tests",
]

osgi_jar_with_tests(
    karaf_command_packages = [
        "org.onosproject.sdniprpki.cli",
        "org.onosproject.sdniprpki.cli.completer",
    ],
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Traffic Engineering",
    description = "Modified SDN-IP peering application with optional support of RPKI Route Validation",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.intentsynchronizer",
        "org.onosproject.route-service",
    ],
    title = "SDN-IP Route Validation",
    url = "http://onosproject.org",
)
