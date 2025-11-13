package com.gotree.API.enums;

/**
 * Define os tipos de eventos que podem ser criados na agenda.
 * - EVENTO: Um evento genérico (ex: Reunião, Feriado).
 * - TREINAMENTO: Um evento específico do tipo Treinamento.
 * - VISITA_REAGENDADA: Uma TechnicalVisit que foi reagendada.
 */
public enum AgendaEventType {
    EVENTO,
    TREINAMENTO,
    VISITA_REAGENDADA
}