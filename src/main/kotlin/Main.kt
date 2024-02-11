import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.get
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import io.ktor.server.plugins.cors.*

import java.lang.Exception
import java.nio.charset.Charset
import java.time.LocalDate
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import javax.sql.DataSource
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

fun hikari(): HikariDataSource {
    val config = HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://localhost:5432/names"
        driverClassName = "org.postgresql.Driver"
        username = "postgres"
        password = "lol"
        maximumPoolSize = 10
        isAutoCommit = true
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }
    return HikariDataSource(config)
}

fun main() {
    embeddedServer(Netty, port = 8080) {
        install(CORS) {
            anyHost()
        }
        module(hikari())
    }.start(wait = true)
}

fun Application.module(dataSource: DataSource) {
    routing {
        get("/data/{query}/{threshold}") {
            val query = call.parameters["query"]

            val connection = dataSource.connection
            try {
                val stmt = connection.createStatement()
                val rs = stmt.executeQuery("""SELECT * FROM (SELECT *, cmp('${query}', first_name, GREATEST(LENGTH(first_name), LENGTH('${query}'))) AS cmp_result FROM names) AS subquery WHERE cmp_result > ${call.parameters["threshold"]} ORDER BY cmp_result DESC LIMIT 10;""")
                var result = "["
                if (rs.next()) {
                    result += """"${rs.getString("first_name")}""""
                }

                while (rs.next()) {
                    // Assuming you're fetching a column named 'name'
                    result += """, "${rs.getString("first_name")}""""
                }

                result += "]"

                call.respondText(result)
            } catch (error: Exception) {
                println(error.toString())
            
            } finally {
                
                connection.close()
            }
        }
    }
}