import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

inline fun DesktopExtension.configureJvmAppDistribution() {
    application {
        mainClass = "$applicationId.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = applicationId
            packageVersion = baseAppVersionString
        }
    }
}