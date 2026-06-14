plugins { alias(libs.plugins.kotlin.jvm) }

kotlin { jvmToolchain(Versions.JAVA.majorVersion.toInt()) }

dependencies { testImplementation(libs.junit) }
