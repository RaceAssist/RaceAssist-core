/*
 *  Copyright © 2021 Nikomaru
 *
 *  This program is free software: you can redistribute it and/or modify
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
package dev.nikomaru.keibaassist.database

import dev.nikomaru.keibaassist.files.Config
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class Database {

    var connection: Connection? = null

    val isConnected: Boolean = connection != null


    @Throws(ClassNotFoundException::class, SQLException::class)
    fun connect() {
        val config = Config()
        if (!isConnected) {
            connection = DriverManager.getConnection(
                "jdbc:mysql://" + config.host + ":" + config.port + "/" + config.database + "?useSSL=false",
                config.username, config.password
            )
            println(config.host + config.port + config.database + config.username + config.password)
        }
    }

    fun disconnect() {
        if (isConnected) {
            try {
                connection!!.close()
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }
    }


    fun initializeDatabase() {
        if(connection == null) {
            connect()
        }
        try {
            val preparedStatement = connection!!.prepareStatement(
                "CREATE TABLE IF NOT EXISTS PlayerList (GroupID VARCHAR(30) NOT NULL,PlayerUUID VARCHAR(40) NOT NULL)"
            )
            preparedStatement.execute()
            preparedStatement.close()
        } catch (ex: SQLException) {
            ex.printStackTrace()
        }
        try {
            val preparedStatement = connection!!.prepareStatement(
                "CREATE TABLE IF NOT EXISTS GroupList (GroupID VARCHAR(30) NOT NULL,PlayerUUID VARCHAR(40) NOT NULL)"
            )
            preparedStatement.execute()
            preparedStatement.close()
        } catch (e: SQLException) {
            e.printStackTrace()
        }
        try {
            val preparedStatement = connection!!.prepareStatement(
                "CREATE TABLE IF NOT EXISTS RaceList (RaceID VARCHAR(30) NOT NULL,Creator VARCHAR(40) NOT NULL)"
            )
            preparedStatement.execute()
            preparedStatement.close()
        } catch (ex: SQLException) {
            ex.printStackTrace()
        }
        try {
            val preparedStatement = connection!!.prepareStatement(
                "CREATE TABLE IF NOT EXISTS CircuitPoint(RaceID VARCHAR(30) NOT NULL,Inside BOOLEAN NOT NULL,XPoint INTEGER NOT NULL,YPoint INTEGER NOT NULL)"
            )
            preparedStatement.execute()
            preparedStatement.close()
        } catch (ex: SQLException) {
            ex.printStackTrace()
        }
    }

}