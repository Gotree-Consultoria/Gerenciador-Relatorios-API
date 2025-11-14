package com.gotree.API.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class RiskCatalog {

    @Data
    @AllArgsConstructor
    public static class RiskItem {
        private int code;
        private String type; // FÍSICO, QUÍMICO, etc.
        private String factor; // Descrição
    }

    public static final List<RiskItem> ALL_RISKS = Arrays.asList(
            new RiskItem(1, "FÍSICO", "Infrassom e sons de baixa frequência"),
            new RiskItem(2, "FÍSICO", "Ruído contínuo ou intermitente"),
            new RiskItem(3, "FÍSICO", "Ruído impulsivo ou de impacto"),
            new RiskItem(4, "FÍSICO", "Ultrassom"),
            new RiskItem(5, "FÍSICO", "Campos magnéticos estáticos"),
            new RiskItem(6, "FÍSICO", "Campos magnéticos de sub-radiofrequência (30 kHz e abaixo)"),
            new RiskItem(7, "FÍSICO", "Sub-radiofrequência (30 kHz e abaixo) e campos eletrostáticos"),
            new RiskItem(8, "FÍSICO", "Radiação de radiofrequência"),
            new RiskItem(9, "FÍSICO", "Micro-ondas"),
            new RiskItem(10, "FÍSICO", "Radiação visível e infravermelho próximo"),
            new RiskItem(11, "FÍSICO", "Radiação ultravioleta, exceto radiação na faixa 400 a 320 nm (Luz Negra)"),
            new RiskItem(12, "FÍSICO", "Radiação ultravioleta na faixa 400 a 320 nm (Luz Negra)"),
            new RiskItem(13, "FÍSICO", "Laser"),
            new RiskItem(14, "FÍSICO", "Radiações ionizantes"),
            new RiskItem(15, "FÍSICO", "Vibrações localizadas (mão-braço)"),
            new RiskItem(16, "FÍSICO", "Vibração de corpo inteiro (aceleração resultante de exposição normalizada – aren)"),
            new RiskItem(17, "FÍSICO", "Frio"),
            new RiskItem(18, "FÍSICO", "Temperaturas anormais (calor)"),
            new RiskItem(19, "FÍSICO", "Pressão hiperbárica"),
            new RiskItem(20, "FÍSICO", "Pressão hipobárica"),
            new RiskItem(21, "FÍSICO", "Vibração de corpo inteiro (Valor da Dose de Vibração Resultante – VDVR)"),
            new RiskItem(22, "QUÍMICO", "Exposição a Fumos Metálicos"),
            new RiskItem(23, "QUÍMICO", "Exposição a Poeira"),
            new RiskItem(24, "QUÍMICO", "Exposição a Produtos Químicos"),
            new RiskItem(25, "BIOLÓGICO", "Agentes biológicos infecciosos e infectocontagiosos"),
            new RiskItem(26, "ERGONÔMICO", "Trabalho em posturas incômodas ou pouco confortáveis por longos períodos"),
            new RiskItem(27, "ERGONÔMICO", "Postura sentada por longos períodos"),
            new RiskItem(28, "ERGONÔMICO", "Postura de pé por longos períodos"),
            new RiskItem(29, "ERGONÔMICO", "Frequente deslocamento a pé durante a jornada de trabalho"),
            new RiskItem(30, "ERGONÔMICO", "Trabalho com esforço físico intenso"),
            new RiskItem(31, "ERGONÔMICO", "Levantamento e transporte manual de cargas ou volumes"),
            new RiskItem(32, "ERGONÔMICO", "Frequente ação de puxar/empurrar cargas ou volumes"),
            new RiskItem(33, "ERGONÔMICO", "Frequente execução de movimentos repetitivos"),
            new RiskItem(34, "ERGONÔMICO", "Manuseio de ferramentas e/ou objetos pesados por longos períodos"),
            new RiskItem(35, "ERGONÔMICO", "Exigência de uso frequente de força, pressão, preensão, flexão, extensão ou torção dos segmentos corporais"),
            new RiskItem(36, "ERGONÔMICO", "Compressão de partes do corpo por superfícies rígidas ou com quinas"),
            new RiskItem(37, "ERGONÔMICO", "Exigência de flexões de coluna vertebral frequentes"),
            new RiskItem(38, "ERGONÔMICO", "Uso frequente de pedais"),
            new RiskItem(39, "ERGONÔMICO", "Uso frequente de alavancas"),
            new RiskItem(40, "ERGONÔMICO", "Exigência de elevação frequente de membros superiores"),
            new RiskItem(41, "ERGONÔMICO", "Manuseio ou movimentação de cargas e volumes sem pega ou com “pega pobre”"),
            new RiskItem(42, "ERGONÔMICO", "Exposição a vibração de corpo inteiro"),
            new RiskItem(43, "ERGONÔMICO", "Exposição a vibrações localizadas (mão-braço)"),
            new RiskItem(44, "ERGONÔMICO", "Uso frequente de escadas"),
            new RiskItem(45, "ERGONÔMICO", "Trabalho intensivo com teclado ou outros dispositivos de entrada de dados"),
            new RiskItem(46, "ERGONÔMICO", "Posto de trabalho improvisado"),
            new RiskItem(47, "ERGONÔMICO", "Mobiliário sem meios de regulagem de ajuste"),
            new RiskItem(48, "ERGONÔMICO", "Equipamentos e/ou máquinas sem meios de regulagem de ajuste ou sem condições de uso"),
            new RiskItem(49, "ERGONÔMICO", "Posto de trabalho não planejado/adaptado para a posição sentada"),
            new RiskItem(50, "ERGONÔMICO", "Assento inadequado"),
            new RiskItem(51, "ERGONÔMICO", "Encosto do assento inadequado ou ausente"),
            new RiskItem(52, "ERGONÔMICO", "Mobiliário ou equipamento sem espaço para movimentação de segmentos corporais"),
            new RiskItem(53, "ERGONÔMICO", "Trabalho com necessidade de alcançar objetos, documentos, controles ou qualquer ponto além das zonas de alcance ideais para as características antropométricas do trabalhador"),
            new RiskItem(54, "ERGONÔMICO", "Equipamentos ou mobiliários não adaptados à antropometria do trabalhador"),
            new RiskItem(55, "ERGONÔMICO", "Condições de trabalho com níveis de pressão sonora fora dos parâmetros de conforto"),
            new RiskItem(56, "ERGONÔMICO", "Condições de trabalho com índice de temperatura efetiva fora dos parâmetros de conforto"),
            new RiskItem(57, "ERGONÔMICO", "Condições de trabalho com velocidade do ar fora dos parâmetros de conforto"),
            new RiskItem(58, "ERGONÔMICO", "Condições de trabalho com umidade do ar fora dos parâmetros de conforto"),
            new RiskItem(59, "ERGONÔMICO", "Condições de trabalho com Iluminação diurna inadequada"),
            new RiskItem(60, "ERGONÔMICO", "Condições de trabalho com Iluminação noturna inadequada"),
            new RiskItem(61, "ERGONÔMICO", "Presença de reflexos em telas, painéis, vidros, monitores ou qualquer superfície, que causem desconforto ou prejudiquem a visualização"),
            new RiskItem(62, "ERGONÔMICO", "Piso escorregadio e/ou irregular"),
            new RiskItem(63, "ERGONÔMICO", "Excesso de situações de estresse"),
            new RiskItem(64, "ERGONÔMICO", "Situações de sobrecarga de trabalho mental"),
            new RiskItem(65, "ERGONÔMICO", "Exigência de alto nível de concentração, atenção e memória"),
            new RiskItem(66, "ERGONÔMICO", "Trabalho em condições de difícil comunicação"),
            new RiskItem(67, "ERGONÔMICO", "Excesso de conflitos hierárquicos no trabalho"),
            new RiskItem(68, "ERGONÔMICO", "Excesso de demandas emocionais/afetivas no trabalho"),
            new RiskItem(69, "ERGONÔMICO", "Assédio de qualquer natureza no trabalho"),
            new RiskItem(70, "ERGONÔMICO", "Trabalho com demandas divergentes (ordens divergentes, metas incompatíveis entre si, entre outras)"),
            new RiskItem(71, "ERGONÔMICO", "Exigência de realização de múltiplas tarefas, com alta demanda cognitiva"),
            new RiskItem(72, "ERGONÔMICO", "Insatisfação no trabalho"),
            new RiskItem(73, "ERGONÔMICO", "Falta de autonomia no trabalho"),
            new RiskItem(74, "ACIDENTE", "Diferença de nível menor ou igual a dois metros"),
            new RiskItem(75, "ACIDENTE", "Diferença de nível maior que dois metros"),
            new RiskItem(76, "ACIDENTE", "Iluminação diurna inadequada"),
            new RiskItem(77, "ACIDENTE", "Iluminação noturna inadequada"),
            new RiskItem(78, "ACIDENTE", "Condições ou procedimentos que possam provocar contato com eletricidade"),
            new RiskItem(79, "ACIDENTE", "Arranjo físico deficiente ou inadequado"),
            new RiskItem(80, "ACIDENTE", "Máquinas e equipamentos sem proteção"),
            new RiskItem(81, "ACIDENTE", "Armazenamento inadequado"),
            new RiskItem(82, "ACIDENTE", "Ferramentas necessitando de ajustes e manutenção"),
            new RiskItem(83, "ACIDENTE", "Ferramentas inadequadas"),
            new RiskItem(84, "ACIDENTE", "Ambientes com risco de engolfamento"),
            new RiskItem(85, "ACIDENTE", "Ambientes com risco de afogamento"),
            new RiskItem(86, "ACIDENTE", "Áreas classificadas"),
            new RiskItem(87, "ACIDENTE", "Queda de objetos"),
            new RiskItem(88, "ACIDENTE", "Intempéries"),
            new RiskItem(89, "ACIDENTE", "Ambientes com risco de soterramento"),
            new RiskItem(90, "ACIDENTE", "Animais peçonhentos"),
            new RiskItem(91, "ACIDENTE", "Animais selvagens"),
            new RiskItem(92, "ACIDENTE", "Mobiliário e/ou superfícies com quinas vivas, rebarbas ou elementos de fixação expostos"),
            new RiskItem(93, "ACIDENTE", "Pisos, passagens, passarelas, plataformas, rampas e corredores com saliências, descontinuidades, aberturas ou obstruções, ou escorregadios"),
            new RiskItem(94, "ACIDENTE", "Escadas e rampas inadequadas"),
            new RiskItem(95, "ACIDENTE", "Superfícies e/ou materiais aquecidos expostos"),
            new RiskItem(96, "ACIDENTE", "Superfícies e/ou materiais em baixa temperatura expostos"),
            new RiskItem(97, "ACIDENTE", "Áreas de trânsito de pedestres ou veículos sem demarcação"),
            new RiskItem(98, "ACIDENTE", "Áreas de movimentação de materiais sem demarcação"),
            new RiskItem(99, "ACIDENTE", "Condução de veículos de qualquer natureza em vias públicas"),
            new RiskItem(100, "ACIDENTE", "Objetos cortantes e/ou perfurocortantes"),
            new RiskItem(101, "ACIDENTE", "Movimentação de materiais"),
            new RiskItem(102, "ACIDENTE", "Máquinas e equipamentos necessitando ajustes e manutenção"),
            new RiskItem(103, "ACIDENTE", "Procedimentos de ajuste, limpeza, manutenção e inspeção deficientes ou inexistentes")
    );

    public static RiskItem getByCode(int code) {
        return ALL_RISKS.stream().filter(r -> r.getCode() == code).findFirst().orElse(null);
    }
}