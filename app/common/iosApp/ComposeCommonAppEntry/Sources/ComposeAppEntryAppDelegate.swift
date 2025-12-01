import CryptoKit
import UIKit
import SwiftUI
import ComposeApp
import Foundation

public final class ComposeAppEntryAppDelegate: NSObject, UIApplicationDelegate, ObservableObject {
    let rootHolder: RootHolder = RootHolder()

    public func application(
        _ application: UIApplication,
        configurationForConnecting connectingSceneSession: UISceneSession,
        options: UIScene.ConnectionOptions
    ) -> UISceneConfiguration {
        let sceneConfig = UISceneConfiguration(name: nil, sessionRole: connectingSceneSession.role)
        sceneConfig.delegateClass = AppSceneDelegate.self
        return sceneConfig
    }

//    func application(_ application: UIApplication, shouldSaveSecureApplicationState coder: NSCoder) -> Bool {
//        rootHolder.saveStateInStateKeeper(coder: coder)
//        return true
//    }
//
//    func application(_ application: UIApplication, shouldRestoreSecureApplicationState coder: NSCoder) -> Bool {
//        rootHolder.restoreStateFromStateKeeper(coder: coder)
//        return true
//    }
}
