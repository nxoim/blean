package com.nxoim.blean.services

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.util.Log

class AccountManagerService : Service() {
    val TAG = "A"
    private lateinit var authenticator: Authenticator

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AccountAuthenticatorService onCreate")
        authenticator = Authenticator(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "AccountAuthenticatorService onBind: ${intent?.action}")
        return authenticator.iBinder
    }

    inner class Authenticator(context: Context) : AbstractAccountAuthenticator(context) {
        override fun editProperties(
            response: AccountAuthenticatorResponse?,
            accountType: String?
        ): Bundle? {
            Log.d(TAG, "Edit propertie")
            TODO("Not yet implemented")
        }

        override fun addAccount(
            response: AccountAuthenticatorResponse?,
            accountType: String?,
            authTokenType: String?,
            requiredFeatures: Array<out String>?,
            options: Bundle?
        ): Bundle {
            Log.d(TAG, "Authenticator addAccount")
            val bundle = Bundle()
            // Here you would typically launch an Activity to handle the account creation flow.
            bundle.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION)
            bundle.putString(AccountManager.KEY_ERROR_MESSAGE, "Add account is not supported")
            return bundle
        }

        override fun confirmCredentials(
            response: AccountAuthenticatorResponse?,
            account: Account?,
            options: Bundle?
        ): Bundle? {
            Log.d(TAG, "Authenticator confirmCredentials")
            return null // Not implemented in this example
        }

        override fun getAuthToken(
            response: AccountAuthenticatorResponse?,
            account: Account?,
            authTokenType: String?,
            options: Bundle?
        ): Bundle {
            Log.d(TAG, "Authenticator getAuthToken")
            val bundle = Bundle()
            bundle.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION)
            bundle.putString(AccountManager.KEY_ERROR_MESSAGE, "Get auth token is not supported")
            return bundle
        }

        override fun getAuthTokenLabel(authTokenType: String?): String? {
            Log.d(TAG, "Authenticator getAuthTokenLabel")
            return null // Not implemented in this example
        }

        override fun hasFeatures(
            response: AccountAuthenticatorResponse?,
            account: Account?,
            features: Array<out String>?
        ): Bundle? {
            Log.d(TAG, "Authenticator hasFeatures")
            return null // Not implemented in this example
        }

        override fun updateCredentials(
            response: AccountAuthenticatorResponse?,
            account: Account?,
            authTokenType: String?,
            options: Bundle?
        ): Bundle? {
            Log.d(TAG, "Authenticator updateCredentials")
            return null // Not implemented in this example
        }
    }
}