package com.gestorgastos.gestor_gastos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String email;

    private String password;

    @Column(name = "google_sub", unique = true)
    private String googleSub;

    @Column(name = "auth_provider", length = 30)
    private String authProvider = "LOCAL";

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;


    // Constructor vacío obligatorio para JPA
    public Usuario() {
    }


    // Constructor con todos los datos necesarios
    public Usuario(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }


    // Getter y Setter de id
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    // Getter y Setter de name
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    // Getter y Setter de email
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    // Getter y Setter de password
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getGoogleSub() {
        return googleSub;
    }

    public void setGoogleSub(String googleSub) {
        this.googleSub = googleSub;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
}
