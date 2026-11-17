package br.com.centralchamados.shared.model

enum class Perfil { SOLICITANTE, ATENDENTE, ADMIN }

enum class Prioridade { BAIXA, MEDIA, ALTA, CRITICA }

enum class StatusChamado { ABERTO, EM_ANDAMENTO, CONCLUIDO }

enum class CanalNotificacao { PUSH, EMAIL }

enum class StatusEnvio { PENDENTE, ENVIADO, FALHOU }
