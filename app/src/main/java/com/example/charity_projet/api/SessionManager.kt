package com.example.charity_projet.api

import android.content.Context
import android.content.SharedPreferences
class SessionManager(context: Context) {

    private var prefs: SharedPreferences = context.getSharedPreferences("charity_prefs", Context.MODE_PRIVATE)

    companion object {
        const val USER_TOKEN = "user_token"
        const val USER_USERNAME = "user_username"    // ⚠️ AJOUT
        const val USER_NAME = "user_name"            // ⚠️ AJOUT
        const val USER_EMAIL = "user_email"          // ⚠️ AJOUT
        const val USER_ROLE = "user_role"
        const val TOKEN_EXPIRY = "token_expiry" // ⚠️ AJOUT
// ⚠️ AJOUT
    }

    fun saveAuthToken(token: String) {
        val editor = prefs.edit()
        editor.putString(USER_TOKEN, token)
        editor.apply()
    }

    // ⚠️ AJOUT: Sauvegarder toutes les infos utilisateur
    fun saveUserInfo(username: String, name: String, email: String, role: String) {
        val editor = prefs.edit()
        editor.putString(USER_USERNAME, username)
        editor.putString(USER_NAME, name)
        editor.putString(USER_EMAIL, email)
        editor.putString(USER_ROLE, role)
        editor.apply()
    }

    fun fetchAuthToken(): String? {
        return prefs.getString(USER_TOKEN, null)
    }

    // ⚠️ AJOUT: Méthodes pour récupérer les infos utilisateur
    fun getUsername(): String? {
        return prefs.getString(USER_USERNAME, null)
    }

    fun getUserName(): String? {
        return prefs.getString(USER_NAME, null)
    }

    fun getUserEmail(): String? {
        return prefs.getString(USER_EMAIL, null)
    }

    fun getUserRole(): String? {
        return prefs.getString(USER_ROLE, null)
    }

    // ⚠️ AJOUT: Vérifier si l'utilisateur est connecté
    fun isLoggedIn(): Boolean {
        return !fetchAuthToken().isNullOrEmpty()
    }

    // ⚠️ CORRECTION: Effacer TOUTES les données d'authentification
    fun clearAuth() {
        val editor = prefs.edit()
        editor.remove(USER_TOKEN)
        editor.remove(USER_USERNAME)    // ⚠️ AJOUT
        editor.remove(USER_NAME)        // ⚠️ AJOUT
        editor.remove(USER_EMAIL)       // ⚠️ AJOUT
        editor.remove(USER_ROLE)        // ⚠️ AJOUT
        editor.apply()
    }

    // ⚠️ Garder l'ancienne méthode pour la compatibilité
    fun clearAuthToken() {
        clearAuth()
    }
}