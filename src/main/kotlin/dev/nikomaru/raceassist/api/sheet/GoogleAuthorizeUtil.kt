/*
 * Copyright © 2021-2022 Nikomaru <nikomaru@nikomaru.dev>
 * This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.nikomaru.raceassist.api.sheet

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.sheets.v4.SheetsScopes
import dev.nikomaru.raceassist.RaceAssist.Companion.plugin
import java.io.*
import java.security.GeneralSecurityException

object GoogleAuthorizeUtil {

    private val credentialsFilePath = File(plugin.dataFolder, "credentials.json")

    @Throws(IOException::class, GeneralSecurityException::class)
    fun authorize(spreadsheetId: String): Credential? {
        val tokensDirectoryPath = File(File(plugin.dataFolder, "tokens"), "${spreadsheetId}_tokens")
        if (!tokensDirectoryPath.exists()) {
            tokensDirectoryPath.mkdirs()
        }
        if (!credentialsFilePath.exists()) {
            return null
        }
        val inputStream: InputStream = FileInputStream(credentialsFilePath)
        val clientSecrets: GoogleClientSecrets = GoogleClientSecrets.load(GsonFactory.getDefaultInstance(), InputStreamReader(inputStream))
        val scopes = listOf(SheetsScopes.SPREADSHEETS)
        val flow: GoogleAuthorizationCodeFlow =
            GoogleAuthorizationCodeFlow.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), clientSecrets, scopes)
                .setDataStoreFactory(FileDataStoreFactory(tokensDirectoryPath)).setAccessType("offline").build()
        return AuthorizationCodeInstalledApp(flow, LocalServerReceiver.Builder().setPort(8888).build()).authorize("user")
    }
}