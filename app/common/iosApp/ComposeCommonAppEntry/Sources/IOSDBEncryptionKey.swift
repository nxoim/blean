import CryptoKit
import SwiftUI
import ComposeApp
import Foundation

class IOSEncryptionKeyManagerImpl: NSObject, IOSEncryptionKeyManager {
    var serviceName: String = ""
    init(serviceName: String) {
        self.serviceName = serviceName
    }

    func getOrCreate(
        key: String,
        keyType: KeyType,
        onSuccess: @escaping (KotlinByteArray) -> Void,
        onFailure: @escaping (KeychainError) -> Void
    ) {
        retrieveKeyData(key: key) { existingKeyData in
            if let keyData = existingKeyData {
                onSuccess(keyData.toKotlinByteArray())
            } else {
                self.createAndSaveKey(
                    key: key,
                    keyType: keyType,
                    onSuccess: onSuccess,
                    onFailure: onFailure
                )
            }
        }
    }

    func replace(
        key: String,
        keyType: KeyType,
        onSuccess: @escaping (KotlinByteArray) -> Void,
        onFailure: @escaping (KeychainError) -> Void
    ) {
        deleteKey(key: key) { success in
            guard success else {
                onFailure(KeychainErrorUnknown())
                return
            }

            self.createAndSaveKey(
                key: key,
                keyType: keyType,
                onSuccess: onSuccess,
                onFailure: onFailure
            )
        }
    }

    func delete(
        key: String,
        onSuccess: @escaping () -> Void,
        onFailure: @escaping (KeychainError) -> Void
    ) {
        deleteKey(key: key) { success in
            if success {
                onSuccess()
            } else {
                onFailure(KeychainErrorNoEntry())
            }
        }
    }

    private func createAndSaveKey(
        key: String,
        keyType: KeyType,
        onSuccess: @escaping (KotlinByteArray) -> Void,
        onFailure: @escaping (KeychainError) -> Void
    ) {
        let keyData: [UInt8]

        switch keyType {
        case let randomBytes as KeyTypeRandomBytes:
            keyData = generateRandomBytes(length: Int(randomBytes.length))
        default:
            onFailure(KeychainErrorUnknown())
            return
        }

        let kotlinByteArray = keyData.toKotlinByteArray()

        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: serviceName,
            kSecAttrAccount as String: key,
            kSecValueData as String: Data(keyData),
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        ]

        let status = SecItemAdd(query as CFDictionary, nil)

        if status == errSecSuccess {
            onSuccess(kotlinByteArray)
        } else {
            logKeychainError(status)
            onFailure(KeychainErrorUnexpectedStatus(status: status))
        }
    }

    private func deleteKey(key: String, completion: @escaping (Bool) -> Void) {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: serviceName,
            kSecAttrAccount as String: key
        ]

        let status = SecItemDelete(query as CFDictionary)
        completion(status == errSecSuccess)
    }

    private func retrieveKeyData(
        key: String,
        completion: @escaping ([UInt8]?) -> Void
    ) {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: serviceName,
            kSecAttrAccount as String: key,
            kSecReturnData as String: true
        ]

        var result: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &result)

        guard status != errSecItemNotFound else {
            completion(nil)
            return
        }

        guard status == errSecSuccess, let data = result as? Data else {
            logKeychainError(status)
            completion(nil)
            return
        }

        completion(Array(data))
    }

    private func generateRandomBytes(length: Int) -> [UInt8] {
        var keyData = [UInt8](repeating: 0, count: length)
        let result = SecRandomCopyBytes(
            kSecRandomDefault,
            keyData.count,
            &keyData
        )

        guard result == errSecSuccess else {
            fatalError("Failed to generate random bytes")
        }

        return keyData
    }

    private func logKeychainError(_ status: OSStatus) {
        if let errorMessage = SecCopyErrorMessageString(status, nil) {
            NSLog("Keychain error: \(errorMessage)")
        }
    }
}

private extension Array where Element == UInt8 {
    func toKotlinByteArray() -> KotlinByteArray {
        let kotlinByteArray = KotlinByteArray(size: Int32(self.count))

        for (index, element) in self.enumerated() {
            kotlinByteArray.set(
                index: Int32(index),
                value: Int8(bitPattern: element)
            )
        }

        return kotlinByteArray
    }
}