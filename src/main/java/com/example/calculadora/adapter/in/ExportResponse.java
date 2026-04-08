package com.example.calculadora.adapter.in;

import lombok.Builder;
import lombok.Value;

/**
 * Payload de resposta da função de exportação.
 * O campo {@code fileContent} contém o .xlsx codificado em Base64.
 */
@Value
@Builder
public class ExportResponse {

    /** Nome sugerido para o arquivo: ex. {@code simulacao-a1b2c3d4.xlsx}. */
    String fileName;

    /** Conteúdo do arquivo .xlsx codificado em Base64. */
    String fileContent;
}
