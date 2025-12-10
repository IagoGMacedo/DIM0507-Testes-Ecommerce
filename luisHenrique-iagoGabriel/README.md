# Autores

Iago Macedo e Luis Scalabrin

# Instrução de Execução

```shell
git clone https://github.com/IagoGMacedo/DIM0507-Testes-Ecommerce.git
cd eCommerce
```

# Testes Obrigatórios para calcularCustoTotal()

#### Cobertura Estrutural


Execute o comando abaixo para gerar o relatório JaCoCo
```shell
mvn verify
```

<img width="1345" height="336" alt="image" src="https://github.com/user-attachments/assets/0a3ca2db-c0af-4864-a66b-507de16b1d2f" />


#### Mutação

Executar com o wrapper do projeto (recomenda-se desabilitar o agente JaCoCo para evitar interferência na instrumentação do PIT):

```bash
./mvnw -Djacoco.skip=true -DskipTests=false org.pitest:pitest-maven:mutationCoverage
```

Como verificar se não restaram mutantes sobreviventes:

1. Abrir o relatório em target/pit-reports/ecommerce.service/index.html
2. Conferir se "Mutation Coverage" está em 100%
3. Abrir target/pit-reports/ecommerce.service/CompraService.java.html e confirmar que não existem entradas com o estado "SURVIVED". Todas devem ser "KILLED"

Principais estratégias utilizadas para matar mutantes:

- Cobertura completa de ramificações de `finalizarCompra`
- Testes de validação e fronteira: `null` e valores-limite para capturar mutadores de fronteira
- Cobertura de streams/lambdas com mocks e `ArgumentCaptor` para validar listas de IDs/quantidades
- Verificação de efeitos colaterais: asserts sobre chamadas externas para matar mutantes que removem chamadas void
- Testes combinados para cálculos para detectar mutações em operações aritméticas e arredondamento
