package com.veterinaria.dao;

import com.veterinaria.modelo.Turno;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;

/**
 * DAO para la entidad Turno.
 * Las consultas usan JOIN FETCH para traer Mascota→Cliente y evitar N+1.
 */
@ApplicationScoped
public class TurnoDAO extends GenericDAO<Turno, Long> {

    @Override
    protected Class<Turno> getEntityClass() {
        return Turno.class;
    }

    // ── Consultas específicas ─────────────────────────────────────

    /**
     * Lista todos los turnos con Mascota y Cliente cargados (JOIN FETCH).
     * Ordenado por fecha descendente (más reciente primero).
     */
    public List<Turno> listarTodos() {
        return em.createNamedQuery("Turno.findAll", Turno.class)
                 .getResultList();
    }

    /**
     * Lista solo los turnos PENDIENTE y CONFIRMADO,
     * ordenados por fecha ascendente (próximo primero).
     */
    public List<Turno> listarVigentes() {
        return em.createNamedQuery("Turno.findVigentes", Turno.class)
                 .getResultList();
    }

    /**
     * Historial de turnos de una mascota específica.
     */
    public List<Turno> listarPorMascota(Long mascotaId) {
        return em.createNamedQuery("Turno.findByMascota", Turno.class)
                 .setParameter("mascotaId", mascotaId)
                 .getResultList();
    }

    /**
     * Turnos filtrados por estado (PENDIENTE, CONFIRMADO, etc.).
     */
    public List<Turno> listarPorEstado(String estado) {
        return em.createNamedQuery("Turno.findByEstado", Turno.class)
                 .setParameter("estado", estado)
                 .getResultList();
    }

    /**
     * Cambia el estado de un turno (PENDIENTE→CONFIRMADO, etc.).
     */
    @Transactional
    public void cambiarEstado(Long id, String nuevoEstado) {
        Turno t = em.find(Turno.class, id);
        if (t != null) {
            t.setEstado(nuevoEstado);
            em.merge(t);
        }
    }
}
