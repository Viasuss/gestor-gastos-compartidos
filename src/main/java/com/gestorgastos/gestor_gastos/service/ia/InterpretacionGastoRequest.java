package com.gestorgastos.gestor_gastos.service.ia;

import java.time.LocalDate;
import java.util.List;

public record InterpretacionGastoRequest(
        Long grupoId,
        String texto,
        int cantidadMiembrosGrupo,
        List<String> nombresMiembros,
        LocalDate fechaReferencia
) {
}
