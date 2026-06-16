package com.veterinaria.dao;

import com.veterinaria.modelo.Cliente;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

/**
 * DAO para la entidad Cliente.
 * Extiende GenericDAO y agrega consultas específicas del dominio.
 *
 * @ApplicationScoped → una sola instancia durante toda la vida
 *                       de la aplicación (CDI scope).
 */
@ApplicationScoped
public class ClienteDAO extends GenericDAO<Cliente, Long> {

    @Override
    protected Class<Cliente> getEntityClass() {
        return Cliente.class;
    }

    // ── Consultas específicas ─────────────────────────────────────

    /**
     * Lista todos los clientes ordenados por apellido y nombre.
     */
    public List<Cliente> listarTodos() {
        return em.createNamedQuery("Cliente.findAll", Cliente.class)
                 .getResultList();
    }

    /**
     * Lista solo clientes con activo = true.
     */
    public List<Cliente> listarActivos() {
        return em.createNamedQuery("Cliente.findActivos", Cliente.class)
                 .getResultList();
    }

    /**
     * Busca un cliente por número de documento exacto.
     * Retorna Optional para manejar la ausencia sin null.
     */
    public Optional<Cliente> buscarPorDocumento(String documento) {
        List<Cliente> resultado =
            em.createNamedQuery("Cliente.findByDocumento", Cliente.class)
              .setParameter("documento", documento)
              .getResultList();
        return resultado.isEmpty()
               ? Optional.empty()
               : Optional.of(resultado.get(0));
    }

    /**
     * Busca un cliente por email exacto.
     */
    public Optional<Cliente> buscarPorEmail(String email) {
        List<Cliente> resultado =
            em.createNamedQuery("Cliente.findByEmail", Cliente.class)
              .setParameter("email", email)
              .getResultList();
        return resultado.isEmpty()
               ? Optional.empty()
               : Optional.of(resultado.get(0));
    }

    /**
     * Búsqueda parcial por nombre, apellido o documento (case-insensitive).
     */
    public List<Cliente> buscarPorTexto(String texto) {
        String patron = "%" + texto.toLowerCase().trim() + "%";
        return em.createNamedQuery("Cliente.buscarPorTexto", Cliente.class)
                 .setParameter("patron", patron)
                 .getResultList();
    }

    /**
     * Baja lógica: marca activo = false sin eliminar el registro.
     */
    @Transactional
    public void desactivar(Long id) {
        Cliente c = em.find(Cliente.class, id);
        if (c != null) {
            c.setActivo(false);
            em.merge(c);
        }
    }

    /**
     * Reactiva un cliente previamente desactivado.
     */
    @Transactional
    public void activar(Long id) {
        Cliente c = em.find(Cliente.class, id);
        if (c != null) {
            c.setActivo(true);
            em.merge(c);
        }
    }
}
