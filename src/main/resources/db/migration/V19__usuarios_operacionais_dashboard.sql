-- Usuarios operacionais com nomes reais para geracao de massa e dashboards.
-- Senha de todos: senha123 (mesmo hash BCrypt usado nos usuarios seed).
INSERT INTO usuarios (username, password, role)
VALUES
    ('Patricia Almeida', '$2a$10$bfASOicSWcwGNT9vTrzbpeVWjlsB5NmXR4xbf4oRJqiyfMa.e1ZEK', 'GESTOR'),
    ('Mariana Souza',   '$2a$10$bfASOicSWcwGNT9vTrzbpeVWjlsB5NmXR4xbf4oRJqiyfMa.e1ZEK', 'ATENDENTE'),
    ('Carlos Silva',    '$2a$10$bfASOicSWcwGNT9vTrzbpeVWjlsB5NmXR4xbf4oRJqiyfMa.e1ZEK', 'MECANICO'),
    ('Rafael Costa',    '$2a$10$bfASOicSWcwGNT9vTrzbpeVWjlsB5NmXR4xbf4oRJqiyfMa.e1ZEK', 'MECANICO'),
    ('Marcos Santos',   '$2a$10$bfASOicSWcwGNT9vTrzbpeVWjlsB5NmXR4xbf4oRJqiyfMa.e1ZEK', 'MECANICO'),
    ('Juliana Lima',    '$2a$10$bfASOicSWcwGNT9vTrzbpeVWjlsB5NmXR4xbf4oRJqiyfMa.e1ZEK', 'MECANICO'),
    ('Fernanda Alves',  '$2a$10$bfASOicSWcwGNT9vTrzbpeVWjlsB5NmXR4xbf4oRJqiyfMa.e1ZEK', 'MECANICO')
ON CONFLICT (username) DO NOTHING;
