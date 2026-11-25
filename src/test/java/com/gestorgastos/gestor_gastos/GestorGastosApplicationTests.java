package com.gestorgastos.gestor_gastos;

import com.gestorgastos.gestor_gastos.support.ExternalProvidersTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(ExternalProvidersTestConfig.class)
class GestorGastosApplicationTests {

	@Test
	void contextLoads() {
	}

}
