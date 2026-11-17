UPDATE setor
SET nome = 'Suporte de Hardware / Infraestrutura',
    descricao = 'Manutenção física, equipamentos, rede local'
WHERE id = '10000000-0000-0000-0000-000000000001';

UPDATE setor
SET nome = 'Desenvolvimento / Sistemas',
    descricao = 'Correção de bugs, ajustes no sistema, falhas técnicas'
WHERE id = '10000000-0000-0000-0000-000000000002';

UPDATE setor
SET nome = 'Comercial / Produtos',
    descricao = 'Criação, ativação e desativação de planos e combos'
WHERE id = '10000000-0000-0000-0000-000000000003';

UPDATE setor
SET nome = 'Suporte Operacional / Administrativo',
    descricao = 'Auxílio às equipes dos provedores'
WHERE id = '10000000-0000-0000-0000-000000000004';

WITH categorias_escopo(setor_id, nome) AS (
    VALUES
        ('10000000-0000-0000-0000-000000000001'::uuid, 'Manutenção física'),
        ('10000000-0000-0000-0000-000000000001'::uuid, 'Equipamentos'),
        ('10000000-0000-0000-0000-000000000001'::uuid, 'Rede local'),
        ('10000000-0000-0000-0000-000000000002'::uuid, 'Correção de bugs'),
        ('10000000-0000-0000-0000-000000000002'::uuid, 'Ajustes no sistema'),
        ('10000000-0000-0000-0000-000000000002'::uuid, 'Falhas técnicas'),
        ('10000000-0000-0000-0000-000000000003'::uuid, 'Criação de planos/combos'),
        ('10000000-0000-0000-0000-000000000003'::uuid, 'Ativação de planos/combos'),
        ('10000000-0000-0000-0000-000000000003'::uuid, 'Desativação de planos/combos'),
        ('10000000-0000-0000-0000-000000000004'::uuid, 'Correção de cadastro'),
        ('10000000-0000-0000-0000-000000000004'::uuid, 'Ajustes de lançamentos'),
        ('10000000-0000-0000-0000-000000000004'::uuid, 'Liberação de pagamentos')
)
UPDATE categoria c
SET ativo = FALSE
WHERE NOT EXISTS (
    SELECT 1
    FROM categorias_escopo ce
    WHERE ce.setor_id = c.setor_id
      AND ce.nome = c.nome
)
AND EXISTS (
    SELECT 1
    FROM chamado ch
    WHERE ch.categoria_id = c.id
);

WITH categorias_escopo(setor_id, nome) AS (
    VALUES
        ('10000000-0000-0000-0000-000000000001'::uuid, 'Manutenção física'),
        ('10000000-0000-0000-0000-000000000001'::uuid, 'Equipamentos'),
        ('10000000-0000-0000-0000-000000000001'::uuid, 'Rede local'),
        ('10000000-0000-0000-0000-000000000002'::uuid, 'Correção de bugs'),
        ('10000000-0000-0000-0000-000000000002'::uuid, 'Ajustes no sistema'),
        ('10000000-0000-0000-0000-000000000002'::uuid, 'Falhas técnicas'),
        ('10000000-0000-0000-0000-000000000003'::uuid, 'Criação de planos/combos'),
        ('10000000-0000-0000-0000-000000000003'::uuid, 'Ativação de planos/combos'),
        ('10000000-0000-0000-0000-000000000003'::uuid, 'Desativação de planos/combos'),
        ('10000000-0000-0000-0000-000000000004'::uuid, 'Correção de cadastro'),
        ('10000000-0000-0000-0000-000000000004'::uuid, 'Ajustes de lançamentos'),
        ('10000000-0000-0000-0000-000000000004'::uuid, 'Liberação de pagamentos')
)
DELETE FROM categoria c
WHERE NOT EXISTS (
    SELECT 1
    FROM categorias_escopo ce
    WHERE ce.setor_id = c.setor_id
      AND ce.nome = c.nome
)
AND NOT EXISTS (
    SELECT 1
    FROM chamado ch
    WHERE ch.categoria_id = c.id
);

INSERT INTO categoria(setor_id, nome) VALUES
    ('10000000-0000-0000-0000-000000000001', 'Manutenção física'),
    ('10000000-0000-0000-0000-000000000001', 'Equipamentos'),
    ('10000000-0000-0000-0000-000000000001', 'Rede local'),
    ('10000000-0000-0000-0000-000000000002', 'Correção de bugs'),
    ('10000000-0000-0000-0000-000000000002', 'Ajustes no sistema'),
    ('10000000-0000-0000-0000-000000000002', 'Falhas técnicas'),
    ('10000000-0000-0000-0000-000000000003', 'Criação de planos/combos'),
    ('10000000-0000-0000-0000-000000000003', 'Ativação de planos/combos'),
    ('10000000-0000-0000-0000-000000000003', 'Desativação de planos/combos'),
    ('10000000-0000-0000-0000-000000000004', 'Correção de cadastro'),
    ('10000000-0000-0000-0000-000000000004', 'Ajustes de lançamentos'),
    ('10000000-0000-0000-0000-000000000004', 'Liberação de pagamentos')
ON CONFLICT (setor_id, nome) DO UPDATE SET ativo = TRUE;
