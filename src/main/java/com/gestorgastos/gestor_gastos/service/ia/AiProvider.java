package com.gestorgastos.gestor_gastos.service.ia;

import java.util.Optional;

public interface AiProvider {

    String getProviderName();

    boolean isAvailable();

    Optional<InterpretacionGastoResponse> interpretar(InterpretacionGastoRequest request);
}
