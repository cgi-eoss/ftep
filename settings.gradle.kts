rootProject.name = "f-tep"

include("f-tep-api")
include("f-tep-batch")
include("f-tep-catalogue")
include("f-tep-clouds")
include("f-tep-costing")
include("f-tep-defaultservices")
include("f-tep-io")
include("f-tep-logging")
include("f-tep-model")
include("f-tep-orchestrator")
include("f-tep-persistence")
include("f-tep-portal")
include("f-tep-queues")
include("f-tep-rpc")
include("f-tep-search")
include("f-tep-security")
include("f-tep-server")
include("f-tep-serviceregistry")
include("f-tep-worker")
include("f-tep-zoolib")
include("f-tep-zoomanager")

include("zoo-project")
project(":zoo-project").projectDir = file("third-party/cxx/zoo-project")

include("jclouds")
project(":jclouds").projectDir = file("third-party/java/jclouds")

include("pkg")
project(":pkg").projectDir = file("third-party/pkg")

include("puppet")
project(":puppet").projectDir = file("third-party/puppet")

include("resto")
project(":resto").projectDir = file("third-party/resto")

include("distribution")
