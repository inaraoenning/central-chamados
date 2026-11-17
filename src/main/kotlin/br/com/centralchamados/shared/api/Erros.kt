package br.com.centralchamados.shared.api

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import java.net.URI

class ErroApi(
    val status: HttpStatus,
    val codigo: String,
    override val message: String,
) : RuntimeException(message)

@RestControllerAdvice
class TratadorDeErros {
    @ExceptionHandler(ErroApi::class)
    fun erroApi(
        e: ErroApi,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> = problema(e.status, e.codigo, e.message, request)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validacao(
        e: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        val detalhe = e.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        val observacaoAusente = e.bindingResult.fieldErrors.any { it.field == "observacaoResolucao" }
        return if (observacaoAusente) {
            problema(HttpStatus.UNPROCESSABLE_ENTITY, "OBSERVACAO_OBRIGATORIA", detalhe, request)
        } else {
            problema(HttpStatus.BAD_REQUEST, "DADOS_INVALIDOS", detalhe, request)
        }
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun constraint(
        e: ConstraintViolationException,
        request: HttpServletRequest,
    ) = problema(HttpStatus.BAD_REQUEST, "DADOS_INVALIDOS", e.message ?: "Dados inválidos", request)

    @ExceptionHandler(OptimisticLockingFailureException::class)
    fun concorrencia(
        e: OptimisticLockingFailureException,
        request: HttpServletRequest,
    ) = problema(HttpStatus.CONFLICT, "CONFLITO_CONCORRENCIA", "O chamado foi alterado por outra pessoa", request)

    @ExceptionHandler(AccessDeniedException::class)
    fun acesso(
        e: AccessDeniedException,
        request: HttpServletRequest,
    ) = problema(HttpStatus.FORBIDDEN, "ACESSO_NEGADO", "Acesso negado", request)

    @ExceptionHandler(ResponseStatusException::class)
    fun status(
        e: ResponseStatusException,
        request: HttpServletRequest,
    ) = problema(HttpStatus.valueOf(e.statusCode.value()), "REQUISICAO_INVALIDA", e.reason ?: "Requisição inválida", request)

    private fun problema(
        status: HttpStatus,
        codigo: String,
        detalhe: String,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetail> {
        val problem = ProblemDetail.forStatusAndDetail(status, detalhe)
        problem.title = status.reasonPhrase
        problem.type = URI.create("https://central-chamados.local/problemas/$codigo")
        problem.instance = URI.create(request.requestURI)
        problem.setProperty("codigo", codigo)
        return ResponseEntity.status(status).body(problem)
    }
}
