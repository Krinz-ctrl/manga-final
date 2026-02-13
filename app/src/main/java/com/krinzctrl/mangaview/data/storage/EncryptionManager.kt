package com.krinzctrl.mangaview.data.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class EncryptionManager(private val context: Context) {
    
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "MangaViewKey"
        private const val AES_GCM_NO_PADDING = "AES/GCM/NoPadding"
        private const val KEY_SIZE = 256
        private const val GCM_IV_LENGTH = 12
    }
    
    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }
    
    init {
        createKeyIfNotExists()
    }
    
    private fun createKeyIfNotExists() {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply {
                init(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_SIZE)
                    .build()
                )
                generateKey()
            }
        }
    }
    
    private fun getSecretKey(): SecretKey {
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }
    
    fun encrypt(input: InputStream, output: OutputStream) {
        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        
        val iv = cipher.iv
        output.write(iv.size)
        output.write(iv)
        
        CipherOutputStream(output, cipher).use { cipherOutput ->
            input.use { inputStream ->
                inputStream.copyTo(cipherOutput)
            }
        }
    }
    
    fun decrypt(input: InputStream): InputStream {
        val ivSize = input.read()
        val iv = ByteArray(ivSize)
        input.read(iv)
        
        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        val spec = GCMParameterSpec(GCM_IV_LENGTH * 8, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        
        return CipherInputStream(input, cipher)
    }
}
