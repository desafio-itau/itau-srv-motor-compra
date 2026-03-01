package com.itau.srv.motor.compras.service;

import com.itau.srv.motor.compras.model.Custodia;
import com.itau.srv.motor.compras.repository.CustodiaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResiduoService - Testes Unitários")
class ResiduoServiceTest {

    @Mock
    private CustodiaRepository custodiaRepository;

    @InjectMocks
    private ResiduoService residuoService;

    private List<Custodia> custodiasMaster;
    private Custodia custodiaMasterPETR4;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(residuoService, "contaMasterId", 1L);

        custodiaMasterPETR4 = new Custodia();
        custodiaMasterPETR4.setId(1L);
        custodiaMasterPETR4.setTicker("PETR4");
        custodiaMasterPETR4.setQuantidade(5);
        custodiaMasterPETR4.setContaGraficaId(1L);
        custodiaMasterPETR4.setPrecoMedio(new BigDecimal("35.00"));

        custodiasMaster = new ArrayList<>();
        custodiasMaster.add(custodiaMasterPETR4);
    }

    @Test
    @DisplayName("Deve ATUALIZAR custodia master quando há resíduo > 0 e custodia existe")
    void deveAtualizarCustodiaMasterQuandoHaResiduoECustodiaExiste() {
        // Arrange
        int residuo = 3;
        BigDecimal precoUnitario = new BigDecimal("36.00");

        when(custodiaRepository.save(any(Custodia.class))).thenReturn(custodiaMasterPETR4);

        // Act
        residuoService.atualizarResiduosCustodiaMaster("PETR4", residuo, precoUnitario, custodiasMaster);

        // Assert
        ArgumentCaptor<Custodia> custodiaCaptor = ArgumentCaptor.forClass(Custodia.class);
        verify(custodiaRepository, times(1)).save(custodiaCaptor.capture());

        Custodia custodiaSalva = custodiaCaptor.getValue();
        assertThat(custodiaSalva.getQuantidade()).isEqualTo(3);
        assertThat(custodiaSalva.getPrecoMedio()).isEqualByComparingTo(new BigDecimal("36.00"));
        assertThat(custodiaSalva.getTicker()).isEqualTo("PETR4");

        verify(custodiaRepository, never()).delete(any(Custodia.class));
    }

    @Test
    @DisplayName("Deve CRIAR custodia master quando há resíduo > 0 e custodia NÃO existe")
    void deveCriarCustodiaMasterQuandoHaResiduoECustodiaNaoExiste() {
        // Arrange
        int residuo = 2;
        BigDecimal precoUnitario = new BigDecimal("62.00");
        List<Custodia> custodiasVazias = new ArrayList<>();

        Custodia novaCustodia = new Custodia();
        when(custodiaRepository.save(any(Custodia.class))).thenReturn(novaCustodia);

        // Act
        residuoService.atualizarResiduosCustodiaMaster("VALE3", residuo, precoUnitario, custodiasVazias);

        // Assert
        ArgumentCaptor<Custodia> custodiaCaptor = ArgumentCaptor.forClass(Custodia.class);
        verify(custodiaRepository, times(1)).save(custodiaCaptor.capture());

        Custodia custodiaCriada = custodiaCaptor.getValue();
        assertThat(custodiaCriada.getTicker()).isEqualTo("VALE3");
        assertThat(custodiaCriada.getQuantidade()).isEqualTo(2);
        assertThat(custodiaCriada.getPrecoMedio()).isEqualByComparingTo(new BigDecimal("62.00"));
        assertThat(custodiaCriada.getContaGraficaId()).isEqualTo(1L);

        assertThat(custodiasVazias).hasSize(1);
        verify(custodiaRepository, never()).delete(any(Custodia.class));
    }

    @Test
    @DisplayName("Deve DELETAR custodia master quando resíduo = 0 e custodia existe")
    void deveDeletarCustodiaMasterQuandoResiduoZeroECustodiaExiste() {
        // Arrange
        int residuo = 0;
        BigDecimal precoUnitario = new BigDecimal("35.00");

        doNothing().when(custodiaRepository).delete(any(Custodia.class));

        // Act
        residuoService.atualizarResiduosCustodiaMaster("PETR4", residuo, precoUnitario, custodiasMaster);

        // Assert
        verify(custodiaRepository, times(1)).delete(custodiaMasterPETR4);
        verify(custodiaRepository, never()).save(any(Custodia.class));
        assertThat(custodiasMaster).isEmpty();
    }

    @Test
    @DisplayName("Deve NADA FAZER quando resíduo = 0 e custodia NÃO existe")
    void deveNadaFazerQuandoResiduoZeroECustodiaNaoExiste() {
        // Arrange
        int residuo = 0;
        BigDecimal precoUnitario = new BigDecimal("30.00");
        List<Custodia> custodiasVazias = new ArrayList<>();

        // Act
        residuoService.atualizarResiduosCustodiaMaster("ITUB4", residuo, precoUnitario, custodiasVazias);

        // Assert
        verify(custodiaRepository, never()).save(any(Custodia.class));
        verify(custodiaRepository, never()).delete(any(Custodia.class));
        assertThat(custodiasVazias).isEmpty();
    }

    @Test
    @DisplayName("Deve processar múltiplos ativos corretamente")
    void deveProcessarMultiplosAtivosCorretamente() {
        // Arrange
        Custodia custodiaMasterVALE3 = new Custodia();
        custodiaMasterVALE3.setId(2L);
        custodiaMasterVALE3.setTicker("VALE3");
        custodiaMasterVALE3.setQuantidade(2);
        custodiaMasterVALE3.setContaGraficaId(1L);
        custodiaMasterVALE3.setPrecoMedio(new BigDecimal("62.00"));

        custodiasMaster.add(custodiaMasterVALE3);

        when(custodiaRepository.save(any(Custodia.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        residuoService.atualizarResiduosCustodiaMaster("PETR4", 1, new BigDecimal("35.50"), custodiasMaster);
        residuoService.atualizarResiduosCustodiaMaster("VALE3", 3, new BigDecimal("63.00"), custodiasMaster);

        // Assert
        verify(custodiaRepository, times(2)).save(any(Custodia.class));

        assertThat(custodiaMasterPETR4.getQuantidade()).isEqualTo(1);
        assertThat(custodiaMasterPETR4.getPrecoMedio()).isEqualByComparingTo(new BigDecimal("35.50"));

        assertThat(custodiaMasterVALE3.getQuantidade()).isEqualTo(3);
        assertThat(custodiaMasterVALE3.getPrecoMedio()).isEqualByComparingTo(new BigDecimal("63.00"));
    }

    @Test
    @DisplayName("Deve adicionar nova custodia à lista após criação")
    void deveAdicionarNovaCustodiaAListaAposCriacao() {
        // Arrange
        int residuo = 5;
        BigDecimal precoUnitario = new BigDecimal("40.00");
        List<Custodia> custodias = new ArrayList<>();

        Custodia novaCustodia = new Custodia();
        novaCustodia.setTicker("WEGE3");
        when(custodiaRepository.save(any(Custodia.class))).thenReturn(novaCustodia);

        // Act
        residuoService.atualizarResiduosCustodiaMaster("WEGE3", residuo, precoUnitario, custodias);

        // Assert
        assertThat(custodias).hasSize(1);
        assertThat(custodias.get(0).getTicker()).isEqualTo("WEGE3");
    }

    @Test
    @DisplayName("Deve remover custodia da lista após deletar")
    void deveRemoverCustodiaDaListaAposDeletar() {
        // Arrange
        assertThat(custodiasMaster).hasSize(1);

        doNothing().when(custodiaRepository).delete(any(Custodia.class));

        // Act
        residuoService.atualizarResiduosCustodiaMaster("PETR4", 0, new BigDecimal("35.00"), custodiasMaster);

        // Assert
        assertThat(custodiasMaster).isEmpty();
        verify(custodiaRepository, times(1)).delete(custodiaMasterPETR4);
    }

    @Test
    @DisplayName("Deve processar resíduo para ativo que não está na lista")
    void deveProcessarResiduoParaAtivoQueNaoEstaNaLista() {
        // Arrange
        int residuo = 10;
        BigDecimal precoUnitario = new BigDecimal("15.00");

        Custodia novaCustodia = new Custodia();
        when(custodiaRepository.save(any(Custodia.class))).thenReturn(novaCustodia);

        // Act
        residuoService.atualizarResiduosCustodiaMaster("BBDC4", residuo, precoUnitario, custodiasMaster);

        // Assert
        ArgumentCaptor<Custodia> custodiaCaptor = ArgumentCaptor.forClass(Custodia.class);
        verify(custodiaRepository, times(1)).save(custodiaCaptor.capture());

        Custodia custodiaCriada = custodiaCaptor.getValue();
        assertThat(custodiaCriada.getTicker()).isEqualTo("BBDC4");
        assertThat(custodiaCriada.getQuantidade()).isEqualTo(10);
        assertThat(custodiasMaster).hasSize(2); // PETR4 + BBDC4
    }

    @Test
    @DisplayName("Deve atualizar preço médio mesmo quando quantidade não muda")
    void deveAtualizarPrecoMedioMesmoQuandoQuantidadeNaoMuda() {
        // Arrange
        custodiaMasterPETR4.setQuantidade(5);
        custodiaMasterPETR4.setPrecoMedio(new BigDecimal("35.00"));

        when(custodiaRepository.save(any(Custodia.class))).thenReturn(custodiaMasterPETR4);

        // Act
        residuoService.atualizarResiduosCustodiaMaster("PETR4", 5, new BigDecimal("36.00"), custodiasMaster);

        // Assert
        ArgumentCaptor<Custodia> custodiaCaptor = ArgumentCaptor.forClass(Custodia.class);
        verify(custodiaRepository, times(1)).save(custodiaCaptor.capture());

        Custodia custodiaSalva = custodiaCaptor.getValue();
        assertThat(custodiaSalva.getQuantidade()).isEqualTo(5);
        assertThat(custodiaSalva.getPrecoMedio()).isEqualByComparingTo(new BigDecimal("36.00"));
    }

    @Test
    @DisplayName("Deve processar resíduo = 1 (caso mínimo)")
    void deveProcessarResiduoMinimoDeUmaAcao() {
        // Arrange
        when(custodiaRepository.save(any(Custodia.class))).thenReturn(custodiaMasterPETR4);

        // Act
        residuoService.atualizarResiduosCustodiaMaster("PETR4", 1, new BigDecimal("35.00"), custodiasMaster);

        // Assert
        ArgumentCaptor<Custodia> custodiaCaptor = ArgumentCaptor.forClass(Custodia.class);
        verify(custodiaRepository, times(1)).save(custodiaCaptor.capture());

        assertThat(custodiaCaptor.getValue().getQuantidade()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve processar resíduo grande (1000 ações)")
    void deveProcessarResiduoGrande() {
        // Arrange
        when(custodiaRepository.save(any(Custodia.class))).thenReturn(custodiaMasterPETR4);

        // Act
        residuoService.atualizarResiduosCustodiaMaster("PETR4", 1000, new BigDecimal("35.00"), custodiasMaster);

        // Assert
        ArgumentCaptor<Custodia> custodiaCaptor = ArgumentCaptor.forClass(Custodia.class);
        verify(custodiaRepository, times(1)).save(custodiaCaptor.capture());

        assertThat(custodiaCaptor.getValue().getQuantidade()).isEqualTo(1000);
    }

    @Test
    @DisplayName("Deve usar contaMasterId configurado ao criar nova custodia")
    void deveUsarContaMasterIdConfiguradoAoCriarNovaCustodia() {
        // Arrange
        ReflectionTestUtils.setField(residuoService, "contaMasterId", 999L);
        List<Custodia> custodiasVazias = new ArrayList<>();

        when(custodiaRepository.save(any(Custodia.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        residuoService.atualizarResiduosCustodiaMaster("VALE3", 10, new BigDecimal("62.00"), custodiasVazias);

        // Assert
        ArgumentCaptor<Custodia> custodiaCaptor = ArgumentCaptor.forClass(Custodia.class);
        verify(custodiaRepository, times(1)).save(custodiaCaptor.capture());

        assertThat(custodiaCaptor.getValue().getContaGraficaId()).isEqualTo(999L);
    }
}

