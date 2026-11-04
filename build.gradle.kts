plugins {
    id("com.android.application") version "9.4.1" apply false
    // Version must match the Kotlin that AGP's built-in Kotlin ships (KGP 2.2.10 in AGP 9.4.1).
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
}
