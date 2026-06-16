package com.veterinaria.modelo;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "turno")
@NamedQueries({
    @NamedQuery(
        name  = "Turno.findAll",
        query = "SELECT t FROM Turno t "
              + "JOIN FETCH t.mascota m "
              + "JOIN FETCH m.cliente "
              + "ORDER BY t.fechaHora DESC"),
    @NamedQuery(
        name  = "Turno.findVigentes",
        query = "SELECT t FROM Turno t "
              + "JOIN FETCH t.mascota m "
              + "JOIN FETCH m.cliente "
              + "WHERE t.estado IN ('PENDIENTE','CONFIRMADO') "
              + "ORDER BY t.fechaHora ASC"),
    @NamedQuery(
        name  = "Turno.findByMascota",
        query = "SELECT t FROM Turno t "
              + "WHERE t.mascota.id = :mascotaId "
              + "ORDER BY t.fechaHora DESC"),
    @NamedQuery(
        name  = "Turno.findByEstado",
        query = "SELECT t FROM Turno t "
              + "JOIN FETCH t.mascota m "
              + "JOIN FETCH m.cliente "
              + "WHERE t.estado = :estado "
              + "ORDER BY t.fechaHora ASC")
})
public class Turno implements Serializable {

    private static final long serialVersionUID = 1L;

    // Constantes de estado para uso en código y vistas
    public static final String PENDIENTE  = "PENDIENTE";
    public static final String CONFIRMADO = "CONFIRMADO";
    public static final String CANCELADO  = "CANCELADO";
    public static final String REALIZADO  = "REALIZADO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "La fecha y hora del turno son obligatorias")
    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Size(max = 200, message = "El motivo no puede superar 200 caracteres")
    @Column(length = 200)
    private String motivo;

    @Column(nullable = false, length = 20)
    private String estado = PENDIENTE;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    // Relación: cada turno pertenece a una mascota
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mascota_id", nullable = false)
    private Mascota mascota;

    // ── Getters y Setters ─────────────────────────────────────────
    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }

    public LocalDateTime getFechaHora()        { return fechaHora; }
    public void setFechaHora(LocalDateTime f)  { this.fechaHora = f; }

    public String getMotivo()                  { return motivo; }
    public void setMotivo(String motivo)       { this.motivo = motivo; }

    public String getEstado()                  { return estado; }
    public void setEstado(String estado)       { this.estado = estado; }

    public String getObservaciones()           { return observaciones; }
    public void setObservaciones(String o)     { this.observaciones = o; }

    public Mascota getMascota()                { return mascota; }
    public void setMascota(Mascota mascota)    { this.mascota = mascota; }

    // ── Helpers booleanos para rendered en vistas ─────────────────
    public boolean isPendiente()   { return PENDIENTE.equals(estado); }
    public boolean isConfirmado()  { return CONFIRMADO.equals(estado); }
    public boolean isCancelado()   { return CANCELADO.equals(estado); }
    public boolean isRealizado()   { return REALIZADO.equals(estado); }

    /** Verdadero si el turno puede ser modificado (no está cerrado) */
    public boolean isModificable() {
        return PENDIENTE.equals(estado) || CONFIRMADO.equals(estado);
    }

    @Override
    public String toString() {
        return "Turno #" + id + " [" + estado + "]";
    }
}
