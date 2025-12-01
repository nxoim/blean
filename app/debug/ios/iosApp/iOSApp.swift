import SwiftUI
import ComposeCommonAppEntry

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor var delegate: ComposeAppEntryAppDelegate
    var body: some Scene {
        return WindowGroup {
            ComposeAppEntry(delegate)
        }
    }
}
