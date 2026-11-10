package model;

import java.time.LocalDate;

/**
 * Documento em PDF vinculado a uma locação (contrato ou recibo). Versões antigas nunca são apagadas (RN29–RN31).
 * "assinatura" resume os dados usados: se nada mudou, não gera uma versão nova.
 */
public record Documento(String tipo, int versao, LocalDate data, String arquivo, String assinatura) { }
