package com.itau.srv.motor.compras.service;

import com.itau.srv.motor.compras.dto.cesta.CestaResponseDTO;
import com.itau.srv.motor.compras.dto.itemcesta.ItemCotacaoAtualResponseDTO;
import com.itau.srv.motor.compras.dto.ordemcompra.CalcularQuantidadeAtivoResponse;
import com.itau.srv.motor.compras.dto.ordemcompra.OrdemCompraResponseDTO;
import com.itau.srv.motor.compras.feign.CestaFeignClient;
import com.itau.srv.motor.compras.mapper.OrdemCompraMapper;
import com.itau.srv.motor.compras.model.Custodia;
import com.itau.srv.motor.compras.model.OrdemCompra;
import com.itau.srv.motor.compras.repository.CustodiaRepository;
import com.itau.srv.motor.compras.repository.OrdemCompraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuantidadeAtivoService - Testes Unitários")
class QuantidadeAtivoServiceTest {

    @Mock
    private CestaFeignClient cestaFeignClient;

    @Mock
    private CustodiaRepository custodiaRepository;

    @Mock
    private OrdemCompraMapper ordemCompraMapper;

    @Mock
    private OrdemCompraRepository ordemCompraRepository;

    @InjectMocks
    private QuantidadeAtivoService quantidadeAtivoService;

    private CestaResponseDTO cestaAtiva;
    private List<ItemCotacaoAtualResponseDTO> itensCesta;
    private LocalDate dataReferencia;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(quantidadeAtivoService, "contaMasterId", 1L);

        itensCesta = List.of(
                new ItemCotacaoAtualResponseDTO("PETR4", new BigDecimal("30"), new BigDecimal("35.00")),
                new ItemCotacaoAtualResponseDTO("VALE3", new BigDecimal("25"), new BigDecimal("62.00")),
                new ItemCotacaoAtualResponseDTO("ITUB4", new BigDecimal("20"), new BigDecimal("30.00")),
                new ItemCotacaoAtualResponseDTO("BBDC4", new BigDecimal("15"), new BigDecimal("15.00")),
                new ItemCotacaoAtualResponseDTO("WEGE3", new BigDecimal("10"), new BigDecimal("40.00"))
        );

        cestaAtiva = new CestaResponseDTO(
                1L,
                "Top Five",
                true,
                LocalDateTime.now(),
                itensCesta
        );

        dataReferencia = LocalDate.now();
    }

    @Test
    @DisplayName("Deve calcular quantidade corretamente sem saldo master")
    void deveCalcularQuantidadeCorretamenteSemSaldoMaster() {
        // Arrange
        BigDecimal valorConsolidado = new BigDecimal("2333.33");

        when(cestaFeignClient.obterCestaAtiva()).thenReturn(cestaAtiva);
        when(custodiaRepository.findCustodiaMaster()).thenReturn(Collections.emptyList());
        when(ordemCompraMapper.mapearParaOrdemCompra(any(), anyLong(), any())).thenAnswer(invocation -> new OrdemCompra());
        when(ordemCompraRepository.save(any(OrdemCompra.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CalcularQuantidadeAtivoResponse resultado = quantidadeAtivoService.calcularQuantidadePorAtivo(valorConsolidado, dataReferencia);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.dtos()).hasSize(5);

        // PETR4: 2333.33 × 0.30 = 699.999 → 699.999 ÷ 35 = 19 ações (TRUNCAR)
        OrdemCompraResponseDTO ordemPETR4 = resultado.dtos().stream()
                .filter(o -> o.ticker().equals("PETR4"))
                .findFirst()
                .orElseThrow();
        assertThat(ordemPETR4.quantidadeTotal()).isEqualTo(19);

        // VALE3: 2333.33 × 0.25 = 583.3325 → 583.3325 ÷ 62 = 9 ações (TRUNCAR)
        OrdemCompraResponseDTO ordemVALE3 = resultado.dtos().stream()
                .filter(o -> o.ticker().equals("VALE3"))
                .findFirst()
                .orElseThrow();
        assertThat(ordemVALE3.quantidadeTotal()).isEqualTo(9);

        verify(cestaFeignClient, times(1)).obterCestaAtiva();
        verify(ordemCompraRepository, times(5)).save(any(OrdemCompra.class));
    }

    @Test
    @DisplayName("Deve descontar saldo master da quantidade a comprar")
    void deveDescontarSaldoMasterDaQuantidadeAComprar() {
        // Arrange
        BigDecimal valorConsolidado = new BigDecimal("2333.33");

        Custodia custodiaMasterPETR4 = new Custodia();
        custodiaMasterPETR4.setTicker("PETR4");
        custodiaMasterPETR4.setQuantidade(5);

        when(cestaFeignClient.obterCestaAtiva()).thenReturn(cestaAtiva);
        when(custodiaRepository.findCustodiaMaster()).thenReturn(List.of(custodiaMasterPETR4));
        when(ordemCompraMapper.mapearParaOrdemCompra(any(), anyLong(), any())).thenAnswer(invocation -> new OrdemCompra());
        when(ordemCompraRepository.save(any(OrdemCompra.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CalcularQuantidadeAtivoResponse resultado = quantidadeAtivoService.calcularQuantidadePorAtivo(valorConsolidado, dataReferencia);

        // Assert
        OrdemCompraResponseDTO ordemPETR4 = resultado.dtos().stream()
                .filter(o -> o.ticker().equals("PETR4"))
                .findFirst()
                .orElseThrow();

        // Quantidade bruta: 19 - Saldo master: 5 = 14 ações a comprar
        assertThat(ordemPETR4.quantidadeTotal()).isEqualTo(14);
    }

    @Test
    @DisplayName("Deve pular compra quando saldo master é suficiente")
    void devePularCompraQuandoSaldoMasterESuficiente() {
        // Arrange
        BigDecimal valorConsolidado = new BigDecimal("2333.33");

        Custodia custodiaMasterPETR4 = new Custodia();
        custodiaMasterPETR4.setTicker("PETR4");
        custodiaMasterPETR4.setQuantidade(20); // Mais que necessário (19)

        when(cestaFeignClient.obterCestaAtiva()).thenReturn(cestaAtiva);
        when(custodiaRepository.findCustodiaMaster()).thenReturn(List.of(custodiaMasterPETR4));
        when(ordemCompraMapper.mapearParaOrdemCompra(any(), anyLong(), any())).thenAnswer(invocation -> new OrdemCompra());
        when(ordemCompraRepository.save(any(OrdemCompra.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CalcularQuantidadeAtivoResponse resultado = quantidadeAtivoService.calcularQuantidadePorAtivo(valorConsolidado, dataReferencia);

        // Assert
        // Deve ter apenas 4 ordens (PETR4 foi pulado)
        assertThat(resultado.dtos()).hasSize(4);
        assertThat(resultado.dtos()).noneMatch(o -> o.ticker().equals("PETR4"));
    }

    @Test
    @DisplayName("Deve usar RoundingMode.DOWN para truncar quantidade")
    void deveUsarRoundingModeDownParaTruncarQuantidade() {
        // Arrange
        BigDecimal valorConsolidado = new BigDecimal("1000.00");

        when(cestaFeignClient.obterCestaAtiva()).thenReturn(cestaAtiva);
        when(custodiaRepository.findCustodiaMaster()).thenReturn(Collections.emptyList());
        when(ordemCompraMapper.mapearParaOrdemCompra(any(), anyLong(), any())).thenAnswer(invocation -> new OrdemCompra());
        when(ordemCompraRepository.save(any(OrdemCompra.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CalcularQuantidadeAtivoResponse resultado = quantidadeAtivoService.calcularQuantidadePorAtivo(valorConsolidado, dataReferencia);

        // Assert
        // PETR4: 1000 × 0.30 = 300 → 300 ÷ 35 = 8.571428... → TRUNCAR = 8
        OrdemCompraResponseDTO ordemPETR4 = resultado.dtos().stream()
                .filter(o -> o.ticker().equals("PETR4"))
                .findFirst()
                .orElseThrow();
        assertThat(ordemPETR4.quantidadeTotal()).isEqualTo(8); // Não 9 (que seria arredondado)
    }

    @Test
    @DisplayName("Deve separar lote padrão e fracionário quando quantidade >= 100")
    void deveSepararLotePadraoEFracionarioQuandoQuantidadeMaiorOuIgual100() {
        // Arrange
        BigDecimal valorConsolidado = new BigDecimal("20000.00"); // Valor alto para gerar 100+ ações

        when(cestaFeignClient.obterCestaAtiva()).thenReturn(cestaAtiva);
        when(custodiaRepository.findCustodiaMaster()).thenReturn(Collections.emptyList());
        when(ordemCompraMapper.mapearParaOrdemCompra(any(), anyLong(), any())).thenAnswer(invocation -> new OrdemCompra());
        when(ordemCompraRepository.save(any(OrdemCompra.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CalcularQuantidadeAtivoResponse resultado = quantidadeAtivoService.calcularQuantidadePorAtivo(valorConsolidado, dataReferencia);

        // Assert
        // PETR4: 20000 × 0.30 = 6000 → 6000 ÷ 35 = 171 ações
        OrdemCompraResponseDTO ordemPETR4 = resultado.dtos().stream()
                .filter(o -> o.ticker().equals("PETR4"))
                .findFirst()
                .orElseThrow();

        assertThat(ordemPETR4.quantidadeTotal()).isEqualTo(171);
        assertThat(ordemPETR4.detalhes()).hasSize(2); // Lote padrão + fracionário
        assertThat(ordemPETR4.detalhes().get(0).tipo()).isEqualTo("LOTE");
        assertThat(ordemPETR4.detalhes().get(0).quantidade()).isEqualTo(100);
        assertThat(ordemPETR4.detalhes().get(1).tipo()).isEqualTo("FRACIONARIO");
        assertThat(ordemPETR4.detalhes().get(1).quantidade()).isEqualTo(71);
    }

    @Test
    @DisplayName("Deve usar apenas fracionário quando quantidade < 100")
    void deveUsarApenasFracionarioQuandoQuantidadeMenor100() {
        // Arrange
        BigDecimal valorConsolidado = new BigDecimal("2333.33");

        when(cestaFeignClient.obterCestaAtiva()).thenReturn(cestaAtiva);
        when(custodiaRepository.findCustodiaMaster()).thenReturn(Collections.emptyList());
        when(ordemCompraMapper.mapearParaOrdemCompra(any(), anyLong(), any())).thenAnswer(invocation -> new OrdemCompra());
        when(ordemCompraRepository.save(any(OrdemCompra.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CalcularQuantidadeAtivoResponse resultado = quantidadeAtivoService.calcularQuantidadePorAtivo(valorConsolidado, dataReferencia);

        // Assert
        OrdemCompraResponseDTO ordemPETR4 = resultado.dtos().stream()
                .filter(o -> o.ticker().equals("PETR4"))
                .findFirst()
                .orElseThrow();

        assertThat(ordemPETR4.quantidadeTotal()).isEqualTo(19);
        assertThat(ordemPETR4.detalhes()).hasSize(1); // Apenas fracionário
        assertThat(ordemPETR4.detalhes().get(0).tipo()).isEqualTo("FRACIONARIO");
        assertThat(ordemPETR4.detalhes().get(0).ticker()).isEqualTo("PETR4F");
    }

    @Test
    @DisplayName("Deve calcular valor total corretamente")
    void deveCalcularValorTotalCorretamente() {
        // Arrange
        BigDecimal valorConsolidado = new BigDecimal("2333.33");

        when(cestaFeignClient.obterCestaAtiva()).thenReturn(cestaAtiva);
        when(custodiaRepository.findCustodiaMaster()).thenReturn(Collections.emptyList());
        when(ordemCompraMapper.mapearParaOrdemCompra(any(), anyLong(), any())).thenAnswer(invocation -> new OrdemCompra());
        when(ordemCompraRepository.save(any(OrdemCompra.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CalcularQuantidadeAtivoResponse resultado = quantidadeAtivoService.calcularQuantidadePorAtivo(valorConsolidado, dataReferencia);

        // Assert
        OrdemCompraResponseDTO ordemPETR4 = resultado.dtos().stream()
                .filter(o -> o.ticker().equals("PETR4"))
                .findFirst()
                .orElseThrow();

        // 19 ações × R$ 35,00 = R$ 665,00
        assertThat(ordemPETR4.valorTotal()).isEqualByComparingTo(new BigDecimal("665.00"));
    }

    @Test
    @DisplayName("Deve processar cesta com um único ativo")
    void deveProcessarCestaComUmUnicoAtivo() {
        // Arrange
        BigDecimal valorConsolidado = new BigDecimal("1000.00");

        List<ItemCotacaoAtualResponseDTO> itensUnico = List.of(
                new ItemCotacaoAtualResponseDTO("PETR4", new BigDecimal("100"), new BigDecimal("35.00"))
        );

        CestaResponseDTO cestaUnica = new CestaResponseDTO(
                1L, "Cesta Única", true, LocalDateTime.now(), itensUnico
        );

        when(cestaFeignClient.obterCestaAtiva()).thenReturn(cestaUnica);
        when(custodiaRepository.findCustodiaMaster()).thenReturn(Collections.emptyList());
        when(ordemCompraMapper.mapearParaOrdemCompra(any(), anyLong(), any())).thenAnswer(invocation -> new OrdemCompra());
        when(ordemCompraRepository.save(any(OrdemCompra.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CalcularQuantidadeAtivoResponse resultado = quantidadeAtivoService.calcularQuantidadePorAtivo(valorConsolidado, dataReferencia);

        // Assert
        assertThat(resultado.dtos()).hasSize(1);

        // 1000 × 1.00 = 1000 → 1000 ÷ 35 = 28 ações
        OrdemCompraResponseDTO ordem = resultado.dtos().get(0);
        assertThat(ordem.ticker()).isEqualTo("PETR4");
        assertThat(ordem.quantidadeTotal()).isEqualTo(28);
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando todos os ativos têm saldo master suficiente")
    void deveRetornarListaVaziaQuandoTodosAtivosTeMSaldoMasterSuficiente() {
        // Arrange
        BigDecimal valorConsolidado = new BigDecimal("100.00"); // Valor muito baixo

        List<Custodia> saldosMasterAltos = new ArrayList<>();
        for (ItemCotacaoAtualResponseDTO item : itensCesta) {
            Custodia custodia = new Custodia();
            custodia.setTicker(item.ticker());
            custodia.setQuantidade(1000); // Muito alto
            saldosMasterAltos.add(custodia);
        }

        when(cestaFeignClient.obterCestaAtiva()).thenReturn(cestaAtiva);
        when(custodiaRepository.findCustodiaMaster()).thenReturn(saldosMasterAltos);

        // Act
        CalcularQuantidadeAtivoResponse resultado = quantidadeAtivoService.calcularQuantidadePorAtivo(valorConsolidado, dataReferencia);

        // Assert
        assertThat(resultado.dtos()).isEmpty();
        verify(ordemCompraRepository, never()).save(any(OrdemCompra.class));
    }

    @Test
    @DisplayName("Deve converter percentual corretamente dividindo por 100")
    void deveConverterPercentualCorretamenteDividindoPor100() {
        // Arrange
        BigDecimal valorConsolidado = new BigDecimal("1000.00");

        List<ItemCotacaoAtualResponseDTO> itensComPercentualInteiro = List.of(
                new ItemCotacaoAtualResponseDTO("PETR4", new BigDecimal("50"), new BigDecimal("100.00"))
        );

        CestaResponseDTO cesta = new CestaResponseDTO(
                1L, "Cesta Teste", true, LocalDateTime.now(), itensComPercentualInteiro
        );

        when(cestaFeignClient.obterCestaAtiva()).thenReturn(cesta);
        when(custodiaRepository.findCustodiaMaster()).thenReturn(Collections.emptyList());
        when(ordemCompraMapper.mapearParaOrdemCompra(any(), anyLong(), any())).thenAnswer(invocation -> new OrdemCompra());
        when(ordemCompraRepository.save(any(OrdemCompra.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CalcularQuantidadeAtivoResponse resultado = quantidadeAtivoService.calcularQuantidadePorAtivo(valorConsolidado, dataReferencia);

        // Assert
        OrdemCompraResponseDTO ordem = resultado.dtos().get(0);

        // 1000 × 0.50 = 500 → 500 ÷ 100 = 5 ações
        // Se não dividisse por 100: 1000 × 50 = 50000 → 50000 ÷ 100 = 500 ações (ERRADO!)
        assertThat(ordem.quantidadeTotal()).isEqualTo(5); // Correto
    }

    @Test
    @DisplayName("Deve salvar todas as ordens no banco")
    void deveSalvarTodasAsOrdensNoBanco() {
        // Arrange
        BigDecimal valorConsolidado = new BigDecimal("2333.33");

        when(cestaFeignClient.obterCestaAtiva()).thenReturn(cestaAtiva);
        when(custodiaRepository.findCustodiaMaster()).thenReturn(Collections.emptyList());
        when(ordemCompraMapper.mapearParaOrdemCompra(any(), anyLong(), any())).thenAnswer(invocation -> new OrdemCompra());
        when(ordemCompraRepository.save(any(OrdemCompra.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CalcularQuantidadeAtivoResponse resultado = quantidadeAtivoService.calcularQuantidadePorAtivo(valorConsolidado, dataReferencia);

        // Assert
        assertThat(resultado.entities()).hasSize(5);
        verify(ordemCompraRepository, times(5)).save(any(OrdemCompra.class));
        verify(ordemCompraMapper, times(5)).mapearParaOrdemCompra(any(), eq(1L), any());
    }
}

