|         Caso        | A (altura ≤ 0/nula) | L (largura ≤ 0/nula) | C (comprimento ≤ 0/nulo) | D = A∨L∨C | Entrada mínima (dimensões)          | Resultado esperado                                   |
| :-----------------: | :-----------------: | :------------------: | :----------------------: | :-------: | ----------------------------------- | ---------------------------------------------------- |
|  **T1** (baseline)  |          F          |           F          |             F            |   **F**   | altura=10, largura=5, comprimento=5 | **Não lança** exceção                                |
| **T2** (prova de A) |        **T**        |           F          |             F            |   **T**   | altura=0, largura=5, comprimento=5  | **Lança** IAE com `erro.produto.dimensoes.invalidas` |
| **T3** (prova de L) |          F          |         **T**        |             F            |   **T**   | altura=10, largura=0, comprimento=5 | **Lança** IAE com `erro.produto.dimensoes.invalidas` |
| **T4** (prova de C) |          F          |           F          |           **T**          |   **T**   | altura=10, largura=5, comprimento=0 | **Lança** IAE com `erro.produto.dimensoes.invalidas` |

