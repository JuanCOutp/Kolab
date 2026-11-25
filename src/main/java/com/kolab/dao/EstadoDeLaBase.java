package com.kolab.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * consultas a la base que no son de ninguna tabla: si responde y cuanto pesa. las usa el monitoreo
 */
@Repository
public class EstadoDeLaBase {

    private final DataSource baseDeDatos;
    private final JdbcTemplate jdbc;

    public EstadoDeLaBase(DataSource baseDeDatos, JdbcTemplate jdbc) {
        this.baseDeDatos = baseDeDatos;
        this.jdbc = jdbc;
    }

    // pide una conexion y le pregunta si sigue viva, con dos segundos de espera como mucho
    public boolean responde() {
        try (Connection conexion = baseDeDatos.getConnection()) {
            return conexion.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    // pg_database_size solo existe en postgresql; en h2, que usan las pruebas, no hay como saberlo
    public Optional<Long> tamanoEnBytes() {
        try (Connection conexion = baseDeDatos.getConnection()) {
            if (!"PostgreSQL".equals(conexion.getMetaData().getDatabaseProductName())) {
                return Optional.empty();
            }
        } catch (SQLException e) {
            return Optional.empty();
        }
        return Optional.ofNullable(jdbc.queryForObject("select pg_database_size(current_database())", Long.class));
    }
}
