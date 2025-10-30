# Execução do projeto

```shell
git clone https://github.com/IagoGMacedo/DIM0507-Testes-Ecommerce.git
cd eCommerce
```

Para executar os testes de caixa branca
```shell
.\mvwn verify
```

Para executar os testes de caixa preta
```shell
.\mvwn test
```

# Caixa Branca

### Cobertura obrigatória de arestas (branch coverage): 
<img width="1345" height="336" alt="image" src="https://github.com/user-attachments/assets/0a3ca2db-c0af-4864-a66b-507de16b1d2f" />


Testes implementados em CompraServiceCoberturaArestasTest

### Cobertura MC/DC (Modified Condition/Decision Coverage):

|         Caso        | A (altura ≤ 0/nula) | L (largura ≤ 0/nula) | C (comprimento ≤ 0/nulo) | D = A∨L∨C | Entrada mínima (dimensões)          | Resultado esperado                                   |
| :-----------------: | :-----------------: | :------------------: | :----------------------: | :-------: | ----------------------------------- | ---------------------------------------------------- |
|  **T1** (baseline)  |          F          |           F          |             F            |   **F**   | altura=10, largura=5, comprimento=5 | **Não lança** exceção                                |
| **T2** (prova de A) |        **T**        |           F          |             F            |   **T**   | altura=0, largura=5, comprimento=5  | **Lança** `erro.produto.dimensoes.invalidas` |
| **T3** (prova de L) |          F          |         **T**        |             F            |   **T**   | altura=10, largura=0, comprimento=5 | **Lança** `erro.produto.dimensoes.invalidas` |
| **T4** (prova de C) |          F          |           F          |           **T**          |   **T**   | altura=10, largura=5, comprimento=0 | **Lança** `erro.produto.dimensoes.invalidas` |

Testes implementados em CompraServiceMCDCTest

### Complexidade e independência de caminhos: 

#### O grafo de fluxo de controle (CFG) do método:
O arquivo se encontra no caminho \eCommerce\documentos\caixaPreta\cfg.png. Por via das dúvidas disponibilizamos ele tanto como png quanto jpg.

#### A complexidade ciclomática (V(G)) calculada:

Temos 95 arestas e 70 nós, dessa forma temos que:

V(G)=E−N+2P=95−70+2(1)=27

#### O número mínimo de casos de teste independentes necessários (≥ V(G)).

Temos que são necessários no minímo 27 casos de teste para garantir cobertura de todas as arestas do método.

# Caixa Preta
Consultar [CasosDeTeste_CalcularCustoTotal.xlsx](documentos/caixaPreta/CasosDeTeste_CalcularCustoTotal.xlsx) para detalhes sobre partições, análise de bordas e tabelas de decisão

PS: Para evitar um número exagerado de colunas, foram feitas duas tabelas de decisão separadas pela dependência de suas condições 