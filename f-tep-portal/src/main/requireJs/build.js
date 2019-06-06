({
    baseUrl: "../resources/app/scripts",
    mainConfigFile: "../resources/app/scripts/main.js",
    name: "app",
    include: ["requireLib"],
    paths: {
        ftepConfig: "empty:" // this is loaded manually, and managed (by Puppet) outside the requireJs context
    },
    preserveLicenseComments: false,
    logLevel: 4,
    optimize: "none",
    generateSourceMaps: false
})
