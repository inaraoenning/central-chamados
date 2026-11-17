package br.com.centralchamados.notificacoes

import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class ExpoPushSender : PushSender {
    private val client = RestClient.builder().baseUrl("https://exp.host/--/api/v2/push/send").build()

    override fun enviar(
        token: String,
        titulo: String,
        mensagem: String,
        dados: Map<String, String>,
    ): ResultadoPush {
        val resposta =
            client
                .post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    mapOf(
                        "to" to token,
                        "title" to titulo,
                        "body" to mensagem,
                        "data" to dados,
                        "sound" to "default",
                    ),
                ).retrieve()
                .body(Map::class.java)
                ?: error("Resposta vazia da Expo Push API")
        val data = resposta["data"] as? Map<*, *> ?: emptyMap<Any, Any>()
        val detalhes = data["details"] as? Map<*, *> ?: emptyMap<Any, Any>()
        val invalido = data["status"] == "error" && detalhes["error"] == "DeviceNotRegistered"
        if (data["status"] == "error" && !invalido) error(data["message"]?.toString() ?: "Falha na Expo Push API")
        return ResultadoPush(aceito = true, dispositivoInvalido = invalido)
    }
}
