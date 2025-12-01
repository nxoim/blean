import CryptoKit
import UIKit
import SwiftUI
import ComposeApp
import Foundation

public struct ComposeAppEntry: View {
    let appDelegate: ComposeAppEntryAppDelegate
    @EnvironmentObject var sceneDelegate: AppSceneDelegate
    
    // Make the initializer public to avoid
    // error: 'ContentView' initializer is inaccessible due to 'internal' protection level
    public init(_ delegate: ComposeAppEntryAppDelegate) {
        self.appDelegate = delegate
    }

    public var body: some View {
        ComposeView(
            instanceManagement: sceneDelegate.sceneInstanceManagement!,
            rootStuff: sceneDelegate.stuff,
            keyManager: appDelegate.rootHolder.keyManager
        )
            .ignoresSafeArea() // Compose has own keyboard handler
    }
}

struct ComposeView: UIViewControllerRepresentable {
    let instanceManagement: PlatformInstanceManagement
    let rootStuff: SceneRootStuff
    let keyManager: IOSEncryptionKeyManager

    func makeUIViewController(context: Context) -> UIViewController {
        let appSupport = try! FileManager.default
            .url(for: .applicationSupportDirectory, in: .userDomainMask, appropriateFor: nil, create: true)
            .appendingPathComponent("blean", isDirectory: true)
            .path
        let cache = FileManager.default
            .urls(for: .cachesDirectory, in: .userDomainMask).first!
            .appendingPathComponent("blean", isDirectory: true)
            .path
        
        return MainViewControllerKt.MainViewController(
            dataStoragePath: appSupport,
            cacheStoragePath: cache,
            defaultComponentContext: rootStuff.defaultComponentContext,
            keyManager: keyManager,
            instanceManagement: instanceManagement
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}
