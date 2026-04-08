package com.example.calculadora.adapter.in;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Payload de entrada da função de exportação. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportRequest {

    String simulationId;
}
