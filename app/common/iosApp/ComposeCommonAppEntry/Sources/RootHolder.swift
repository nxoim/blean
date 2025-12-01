import CryptoKit
import UIKit
import SwiftUI
import ComposeApp
import Foundation

class RootHolder: ObservableObject {
    let lifecycleRegistry: LifecycleRegistry
    private var appStateKeeper = StateKeeperDispatcherKt.StateKeeperDispatcher(savedState: nil)
    let defaultComponentContext: DefaultComponentContext

    var keyManager = IOSEncryptionKeyManagerImpl(serviceName: "com.nxoim.blean")

    init() {
        lifecycleRegistry = LifecycleRegistryKt.LifecycleRegistry()

        defaultComponentContext = DefaultComponentContext(
            lifecycle: lifecycleRegistry,
            stateKeeper: appStateKeeper,
            instanceKeeper: nil,
            backHandler: nil
        )

        LifecycleRegistryExtKt.create(lifecycleRegistry)
    }

    deinit {
        // Destroy the root component before it is deallocated
        LifecycleRegistryExtKt.destroy(lifecycleRegistry)
    }

    func saveStateInStateKeeper(coder: NSCoder) {
//        StateKeeperUtilsKt.save(coder: coder, state: appStateKeeper.save())
    }

    func restoreStateFromStateKeeper(coder: NSCoder) {
//        appStateKeeper = StateKeeperDispatcherKt.StateKeeperDispatcher(
//            savedState: StateKeeperUtilsKt.restore(coder: coder)
//        )
    }
}

