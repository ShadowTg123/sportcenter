-- Configuración inicial de la base de datos y la tabla de usuarios.
CREATE DATABASE IF NOT EXISTS sportcenter
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE sportcenter;

CREATE TABLE IF NOT EXISTS USUARIO (
    idUsuario INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL,
    usuario VARCHAR(30) NOT NULL UNIQUE,
    clave VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL
);

DESCRIBE sportcenter.USUARIO;