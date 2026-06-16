package com.veterinaria.modelo;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mascota")
@NamedQueries({
    @NamedQuery(
        name  = "Mascota.findAll",
        query = "SELECT m FROM Mascota m JOIN FETCH m.cliente ORDER BY m.nombre"),
    @NamedQuery(
        name  = "Mascota.findActivas",
        query = "SELECT m FROM Mascota m JOIN FETCH m.cliente "
              + "WHERE m.activo = true ORDER BY m.nombre"),
    @NamedQuery(
        name  = "Mascota.findByCliente",
        query = "SELECT m FROM Mascota m "
              + "WHERE m.cliente.id = :clienteId AND m.activo = true "
              + "ORDER BY m.nombre"),
    @NamedQuery(
        name  = "Mascota.buscarPorNombre",
        query = "SELECT m FROM Mascota m JOIN FETCH m.cliente "
              + "WHERE LOWER(m.nombre) LIKE :patron ORDER BY m.nombre")
})
public class Mascota implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre de la mascota es obligatorio")
    @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
    @Column(nullable = false, length = 80)
    private String nombre;

    @NotBlank(message = "La especie es obligatoria")
    @Size(max = 50, message = "La especie no puede superar 50 caracteres")
    @Column(nullable = false, length = 50)
    private String especie;

    @Size(max = 100, message = "La raza no puede superar 100 caracteres")
    @Column(length = 100)
    private String raza;

    @Column(name = "fecha_nac")
    private LocalDate fechaNac;

    @Column(length = 1)
    private String sexo;   // "M" = Macho  |  "H" = Hembra

    @Column(nullable = false)
    private boolean activo = true;

    // Relación: cada mascota pertenece a un cliente
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    // Relación: una mascota tiene muchos turnos
    @OneToMany(
        mappedBy      = "mascota",
        cascade       = CascadeType.ALL,
        fetch         = FetchType.LAZY,
        orphanRemoval = true
    )
    private List<Turno> turnos = new ArrayList<>();

    // ── Getters y Setters ─────────────────────────────────────────
    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }

    public String getNombre()                  { return nombre; }
    public void setNombre(String nombre)       { this.nombre = nombre; }

    public String getEspecie()                 { return especie; }
    public void setEspecie(String especie)     { this.especie = especie; }

    public String getRaza()                    { return raza; }
    public void setRaza(String raza)           { this.raza = raza; }

    public LocalDate getFechaNac()             { return fechaNac; }
    public void setFechaNac(LocalDate f)       { this.fechaNac = f; }

    public String getSexo()                    { return sexo; }
    public void setSexo(String sexo)           { this.sexo = sexo; }

    public boolean isActivo()                  { return activo; }
    public void setActivo(boolean activo)      { this.activo = activo; }

    public Cliente getCliente()                { return cliente; }
    public void setCliente(Cliente cliente)    { this.cliente = cliente; }

    public List<Turno> getTurnos()             { return turnos; }
    public void setTurnos(List<Turno> t)       { this.turnos = t; }

    // ── Helpers de vista ──────────────────────────────────────────
    public String getSexoDescripcion() {
        if ("M".equals(sexo)) return "Macho";
        if ("H".equals(sexo)) return "Hembra";
        return "—";
    }

    public String getNombreConEspecie() {
        return nombre + " (" + especie + ")";
    }

    /** Muestra nombre de la mascota con el apellido del dueño */
    public String getNombreConDueno() {
        if (cliente != null) {
            return nombre + " — " + cliente.getApellido() + ", " + cliente.getNombre();
        }
        return nombre;
    }

    @Override
    public String toString() {
        return nombre + " (" + especie + ")";
    }
}
