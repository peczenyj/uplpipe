resolvers += "Web plugin repo" at "http://repo1.maven.org/maven2"

libraryDependencies <+= sbtVersion(v => "com.github.siasia" %% "xsbt-web-plugin" % (v+"-0.2.8"))