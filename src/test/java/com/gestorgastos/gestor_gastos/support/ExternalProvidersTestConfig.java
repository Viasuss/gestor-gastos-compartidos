package com.gestorgastos.gestor_gastos.support;

import com.gestorgastos.gestor_gastos.service.GoogleAuthService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Configuracion de mocks para proveedores externos en pruebas (Correccion C3).
 *
 * Reemplaza GoogleAuthService por un mock de Mockito para garantizar cero
 * llamadas HTTP salientes durante las pruebas de contexto y controladores.
 */
@TestConfiguration
public class ExternalProvidersTestConfig {

    @Bean
    @Primary
    public GoogleAuthService googleAuthService() {
        GoogleAuthService mockService = Mockito.mock(GoogleAuthService.class);
        when(mockService.verificarToken(anyString())).thenReturn(Optional.empty());
        when(mockService.getGoogleClientId()).thenReturn("TEST_MOCK_GOOGLE_CLIENT_ID");
        return mockService;
    }
}
