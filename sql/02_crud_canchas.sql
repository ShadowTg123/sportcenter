USE sportcenter;

CREATE TABLE IF NOT EXISTS CANCHA (
                                      idCancha INT AUTO_INCREMENT PRIMARY KEY,
                                      nombre VARCHAR(50) NOT NULL,
    deporte VARCHAR(30) NOT NULL,
    tarifaHora DECIMAL(6,2) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'disponible',

    CONSTRAINT chk_cancha_deporte
    CHECK (deporte IN ('Fútbol', 'Vóley')),

    CONSTRAINT chk_cancha_tarifa
    CHECK (tarifaHora BETWEEN 10.00 AND 500.00),

    CONSTRAINT chk_cancha_estado
    CHECK (estado IN (
           'disponible',
           'alquilada',
           'mantenimiento',
           'inactiva'
                     ))
    );

CREATE TABLE IF NOT EXISTS CLIENTE (
                                       idCliente INT AUTO_INCREMENT PRIMARY KEY,
                                       nombre VARCHAR(80) NOT NULL,
    dni VARCHAR(8) NOT NULL,
    telefono VARCHAR(9),

    CONSTRAINT uq_cliente_dni UNIQUE (dni)
    );

CREATE TABLE IF NOT EXISTS ALQUILER (
                                        idAlquiler INT AUTO_INCREMENT PRIMARY KEY,
                                        idCancha INT NOT NULL,
                                        idCliente INT NOT NULL,
                                        idUsuario INT NOT NULL,
                                        fecha DATE NOT NULL,
                                        horaInicio TIME NOT NULL,
                                        horaFin TIME NOT NULL,
                                        montoTotal DECIMAL(8,2) NOT NULL,
    estado VARCHAR(20) NOT NULL,

    CONSTRAINT fk_alquiler_cancha
    FOREIGN KEY (idCancha) REFERENCES CANCHA(idCancha),

    CONSTRAINT fk_alquiler_cliente
    FOREIGN KEY (idCliente) REFERENCES CLIENTE(idCliente),

    CONSTRAINT fk_alquiler_usuario
    FOREIGN KEY (idUsuario) REFERENCES USUARIO(idUsuario),

    CONSTRAINT chk_alquiler_horario
    CHECK (horaFin > horaInicio),

    CONSTRAINT chk_alquiler_monto
    CHECK (montoTotal >= 0),

    CONSTRAINT chk_alquiler_estado
    CHECK (estado IN ('confirmado', 'cancelado', 'completado'))
    );