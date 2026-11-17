package br.com.centralchamados.auth

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

@org.springframework.stereotype.Component
class JwtAuthenticationFilter(
    private val authService: AuthService,
) : org.springframework.web.filter.OncePerRequestFilter() {
    override fun doFilterInternal(
        request: jakarta.servlet.http.HttpServletRequest,
        response: jakarta.servlet.http.HttpServletResponse,
        filterChain: jakarta.servlet.FilterChain,
    ) {
        val bearer = request.getHeader("Authorization")?.takeIf { it.startsWith("Bearer ", true) }?.substringAfter(' ')
        val principal = bearer?.let(authService::validarAccessToken)
        if (principal != null) {
            val auth =
                UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    listOf(
                        org.springframework.security.core.authority
                            .SimpleGrantedAuthority("ROLE_${principal.perfil}"),
                    ),
                )
            SecurityContextHolder.getContext().authentication = auth
        }
        filterChain.doFilter(request, response)
    }
}
