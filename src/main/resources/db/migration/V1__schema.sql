CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE provedor (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(160) NOT NULL,
    cnpj VARCHAR(14) UNIQUE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE setor (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(160) NOT NULL UNIQUE,
    descricao VARCHAR(500),
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE categoria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    setor_id UUID NOT NULL REFERENCES setor(id),
    nome VARCHAR(160) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE(setor_id, nome)
);

CREATE TABLE usuario (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(160) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    senha_hash VARCHAR(100) NOT NULL,
    perfil VARCHAR(20) NOT NULL CHECK (perfil IN ('SOLICITANTE', 'ATENDENTE', 'ADMIN')),
    provedor_id UUID REFERENCES provedor(id),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK ((perfil = 'SOLICITANTE' AND provedor_id IS NOT NULL) OR perfil <> 'SOLICITANTE')
);

CREATE TABLE usuario_setor (
    usuario_id UUID NOT NULL REFERENCES usuario(id),
    setor_id UUID NOT NULL REFERENCES setor(id),
    PRIMARY KEY(usuario_id, setor_id)
);

CREATE SEQUENCE chamado_seq;

CREATE TABLE chamado (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    numero VARCHAR(20) NOT NULL UNIQUE,
    titulo VARCHAR(200) NOT NULL,
    descricao VARCHAR(5000) NOT NULL,
    setor_id UUID NOT NULL REFERENCES setor(id),
    categoria_id UUID NOT NULL REFERENCES categoria(id),
    prioridade VARCHAR(10) NOT NULL CHECK (prioridade IN ('BAIXA', 'MEDIA', 'ALTA', 'CRITICA')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ABERTO', 'EM_ANDAMENTO', 'CONCLUIDO')),
    solicitante_id UUID NOT NULL REFERENCES usuario(id),
    provedor_id UUID NOT NULL REFERENCES provedor(id),
    atendente_id UUID REFERENCES usuario(id),
    previsao_resolucao TIMESTAMPTZ,
    observacao_resolucao VARCHAR(5000),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    assumido_em TIMESTAMPTZ,
    concluido_em TIMESTAMPTZ,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_chamado_fila ON chamado(setor_id, status, prioridade, criado_em);
CREATE INDEX idx_chamado_solicitante ON chamado(solicitante_id, criado_em);
CREATE INDEX idx_chamado_provedor ON chamado(provedor_id, criado_em);

CREATE TABLE chamado_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chamado_id UUID NOT NULL REFERENCES chamado(id),
    tipo_evento VARCHAR(40) NOT NULL,
    autor_id UUID REFERENCES usuario(id),
    dados JSONB NOT NULL DEFAULT '{}'::jsonb,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_chamado_log_chamado ON chamado_log(chamado_id, criado_em);
CREATE FUNCTION impedir_alteracao_chamado_log() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'chamado_log is append-only' USING ERRCODE = '55000';
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_chamado_log_imutavel
    BEFORE UPDATE OR DELETE ON chamado_log
    FOR EACH ROW EXECUTE FUNCTION impedir_alteracao_chamado_log();

CREATE TABLE notificacao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    destinatario_id UUID NOT NULL REFERENCES usuario(id),
    chamado_id UUID NOT NULL REFERENCES chamado(id),
    tipo_evento VARCHAR(60) NOT NULL,
    titulo VARCHAR(200) NOT NULL,
    mensagem VARCHAR(1000) NOT NULL,
    lida BOOLEAN NOT NULL DEFAULT FALSE,
    lida_em TIMESTAMPTZ,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    alerta_agendado BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_notificacao_nao_lida ON notificacao(destinatario_id, lida);
CREATE UNIQUE INDEX uq_alerta_chamado_evento_destinatario
    ON notificacao(chamado_id, tipo_evento, destinatario_id) WHERE alerta_agendado;

CREATE TABLE notificacao_envio (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notificacao_id UUID NOT NULL REFERENCES notificacao(id),
    canal VARCHAR(10) NOT NULL CHECK (canal IN ('PUSH', 'EMAIL')),
    status VARCHAR(10) NOT NULL DEFAULT 'PENDENTE' CHECK (status IN ('PENDENTE', 'ENVIADO', 'FALHOU')),
    tentativas INTEGER NOT NULL DEFAULT 0,
    erro VARCHAR(1000),
    enviado_em TIMESTAMPTZ,
    proxima_tentativa_em TIMESTAMPTZ
);
CREATE INDEX idx_notificacao_envio_pendente ON notificacao_envio(status, tentativas);

CREATE TABLE preferencia_notificacao (
    usuario_id UUID NOT NULL REFERENCES usuario(id),
    tipo_evento VARCHAR(60) NOT NULL,
    canal VARCHAR(10) NOT NULL CHECK (canal IN ('PUSH', 'EMAIL')),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY(usuario_id, tipo_evento, canal)
);

CREATE TABLE dispositivo_push (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuario(id),
    token VARCHAR(500) NOT NULL UNIQUE,
    plataforma VARCHAR(20) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE configuracao_notificacao (
    chave VARCHAR(100) PRIMARY KEY,
    valor VARCHAR(500) NOT NULL
);
CREATE TABLE sessao_refresh (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuario(id),
    hash VARCHAR(64) NOT NULL UNIQUE,
    expira_em TIMESTAMPTZ NOT NULL,
    revogada_em TIMESTAMPTZ
);
INSERT INTO configuracao_notificacao(chave, valor) VALUES
    ('minutos_alerta_previsao', '60'),
    ('minutos_alerta_sem_atendimento', '30');

INSERT INTO setor(id, nome, descricao) VALUES
    ('10000000-0000-0000-0000-000000000001', 'Suporte de Hardware', 'Infraestrutura'),
    ('10000000-0000-0000-0000-000000000002', 'Desenvolvimento', 'Sistemas'),
    ('10000000-0000-0000-0000-000000000003', 'Comercial', 'Produtos'),
    ('10000000-0000-0000-0000-000000000004', 'Suporte Operacional', 'Administrativo');
INSERT INTO categoria(setor_id, nome) VALUES
    ('10000000-0000-0000-0000-000000000001', 'Equipamentos'),
    ('10000000-0000-0000-0000-000000000001', 'Rede interna'),
    ('10000000-0000-0000-0000-000000000002', 'Aplicação'),
    ('10000000-0000-0000-0000-000000000002', 'Integração'),
    ('10000000-0000-0000-0000-000000000003', 'Planos e produtos'),
    ('10000000-0000-0000-0000-000000000003', 'Campanhas'),
    ('10000000-0000-0000-0000-000000000004', 'Processos'),
    ('10000000-0000-0000-0000-000000000004', 'Acesso interno');
