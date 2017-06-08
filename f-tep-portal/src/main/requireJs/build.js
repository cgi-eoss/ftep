({
    baseUrl: "../resources/app/scripts",
    mainConfigFile: "../resources/app/scripts/main.js",
    name: "app",
    include: ["requireLib"],
    paths: {
        ftepConfig: "empty:" // this is loaded manually, and managed (by Puppet) outside the requireJs context
    },
    preserveLicenseComments: false,
    optimize: "closure",
    closure: {
        CompilerOptions: {
            languageIn: com.google.javascript.jscomp.CompilerOptions.LanguageMode.ECMASCRIPT5
        }
    },
    generateSourceMaps: true
})