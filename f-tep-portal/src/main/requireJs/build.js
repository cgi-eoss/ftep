({
    baseUrl: "../resources/app/scripts",
    mainConfigFile: "../resources/app/scripts/main.js",
    name: "app",
    include: ["requireLib"],
    paths: {
        ftepConfig: "empty:" // this is loaded manually, and managed (by Puppet) outside the requireJs context
    },
    optimize: "none"
})