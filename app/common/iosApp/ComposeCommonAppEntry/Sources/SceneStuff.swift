import CryptoKit
import UIKit
import SwiftUI
import ComposeApp
import Foundation

class AppSceneDelegate: NSObject, UIWindowSceneDelegate, ObservableObject {
    let lifecycleRegistry = LifecycleRegistryKt.LifecycleRegistry()
    var sceneInstanceManagement: IOSSceneInstanceManagement? = nil
    var stuff: SceneRootStuff {
        SceneRootStuff(
            lifecycleRegistry: lifecycleRegistry,
            defaultComponentContext: DefaultComponentContext(lifecycle: lifecycleRegistry)
        )
    }

    deinit {
        LifecycleRegistryExtKt.destroy(stuff.lifecycleRegistry)
    }

    func scene(
        _ scene: UIScene,
        willConnectTo session: UISceneSession,
        options connectionOptions: UIScene.ConnectionOptions
    ) {
        print("new scene")

        lifecycleRegistry.subscribe(callbacks: SceneLifecycleCallbacks(sceneSession: session))
        sceneInstanceManagement = IOSSceneInstanceManagement(session: session)

        LifecycleRegistryExtKt.create(stuff.lifecycleRegistry)
    }

    func sceneDidEnterBackground(_ scene: UIScene) {
        if stuff.lifecycleRegistry.state == .resumed {
            stuff.lifecycleRegistry.onPause()
        }
    }

    func sceneDidBecomeActive(_ scene: UIScene) {
        if stuff.lifecycleRegistry.state == .started {
            stuff.lifecycleRegistry.onResume()
        }

        if stuff.lifecycleRegistry.state == .created {
            stuff.lifecycleRegistry.onStart()
            stuff.lifecycleRegistry.onResume()
        }
    }

    func sceneDidDisconnect(_ scene: UIScene) {
        print("scene disconnected")
        if stuff.lifecycleRegistry.state == .started {
            stuff.lifecycleRegistry.onStop()
        }
    }
}

class IOSSceneInstanceManagement : PlatformInstanceManagement {
    let session: UISceneSession
    init(session: UISceneSession) {
        self.session = session
    }

    func close() {
        UIApplication.shared.requestSceneSessionDestruction(
            session,
            options: .none
        )
    }
}

class SceneLifecycleCallbacks: LifecycleCallbacks {

    let sceneSession: UISceneSession

    init(sceneSession: UISceneSession) {
        self.sceneSession = sceneSession
    }

    func onCreate() {
    }

    func onDestroy() {
        UIApplication.shared.requestSceneSessionDestruction(sceneSession, options: .none)
    }

    func onPause() {
    }

    func onResume() {
    }

    func onStart() {
    }

    func onStop() {
    }
}

extension UIApplication {
    func closeScene(for view: UIView) {
        if let windowScene = view.window?.windowScene {
            requestSceneSessionDestruction(windowScene.session, options: nil)
        }
    }
}

struct SceneRootStuff {
    let lifecycleRegistry: LifecycleRegistry
    let defaultComponentContext: DefaultComponentContext
}
