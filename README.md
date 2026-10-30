# Order Book em Java (Arquitetura High Frequency Trading)

Este projeto consiste em um motor de negociação (Matching Engine) de ultra-baixa latência capaz de fazer o fluxo completo de processamento de ordens, desde o envio da ordem até a sua escrita no banco de dados. O sistema implementa um Order Book completo, com Multithreading, arquitetura Lock-Free, persistência assíncrona em lote, conectividade via rede e telemetria em tempo real.



## Checklist dos requisitos do exercício proposto IED Strats Morgan Stanley. 
### Todos os 5 requisitos e os 5 requisitos adicionais são atendidos. As entradas devem ser feitas como no exercício proposto e as saídas contêm todas as informações orientadas. Abaixo estão comentários sobre cada requisito/requisito adicional e outras observações.

### Requisitos:
1. Engine trabalha com somente 1 ativo.
2. Suporta ordens limit, market e pegged.
3. As informações persistentes são mantidas em um banco de dados SQLite (`.db`) no disco rígido. Durante a inicialização, o sistema realiza a reconstrução do Order Book, carregando para a memória RAM (volátil) apenas as ordens ativas. O motor também restaura os rastreadores dinâmicos de preço (como o mapeamento de ordens Pegged), garantindo que o estado seja recuperado plenamente após uma reinicialização. Ordens inativas (canceladas ou totalmente executadas) permanecem apenas no disco para manter o livro e a memória otimizados, preservando o histórico para auditoria.
4. Foi implementado um servidor de rede via Sockets TCP, replicando o fluxo de dados em streaming característico de conexões reais com bolsas de valores (como é o caso do protocolo FIX). Não foram utilizadas ferramentas de nuvem ou elasticidade.
5. A sintaxe de recebimento, cancelamento e modificação segue estritamente os padrões do enunciado. A saída no terminal foi formatada para reportar as execuções com todas as informações que o enunciado pede.

### Requisitos adicionais:
1. A visualização do livro (`print book`) segue o padrão de market data *Market-By-Order*: exibe a lista de Asks em ordem decrescente, o *Spread* centralizado, e a lista de Bids em ordem natural, mantendo o *Best Ask* e o *Best Bid* (topo do livro) sempre ao centro.
2. A *Price-Time Priority* é matematicamente garantida:
    - **Prioridade de Preço:** `TreeMaps` garantem o ordenamento automático dos níveis de preço com inserção/busca/remoção em O(log N).
    - **Prioridade de Tempo:** Dentro de cada nível de preço, uma Lista Duplamente Encadeada customizada (`PriceLevelQueue`) garante processamento estritamente FIFO (O(1)).
    - **Rastreamento O(1):** Um `HashMap` mapeia os IDs das ordens, o que garante inserção/execução/cancelamento de uma ordem na fila em O(1).
3. O cancelamento remove a ordem instantaneamente do motor em memória, mas submete uma instrução assíncrona que marca o status como `CANCELLED` no banco de dados, preservando a trilha de auditoria idêntica às bolsas reais.
4. A modificação de ordens aplica o padrão *Cancel and Replace*: a ordem original é cancelada e perde a sua prioridade na fila, gerando uma nova ordem com ID e parâmetros atualizados no fim da fila do nível de preço escolhido.
5. Ordens *Pegged* (peg bid buy / peg offer sell) atuam como *Liquidity Makers*, rastreando automaticamente o topo do livro de ofertas e se reposicionando sempre que o melhor preço do ativo sofre alteração. Caso a última ordem limit de um lado do livro seja cancelada ou executada e só sobre ordens pegged, é feito o Parking e elas são congeladas no último preço que ela rastreou, e reativadas quando é adicionada uma ordem limit novamente.

### Outras observações:
1. Ordens limit que cruzem o spread (geram trades imediatamente) são preenchidas (tratadas como Takers), replicando o comportamento padrão da maioria das bolsas globais (como B3, NYSE, Nasdaq) para ordens limit.
2. Opera-se internamente nos preços como se fossem inteiros (30.00 -> 3000) para evitar imprecisões de ponto flutuante e são guardados no banco de dados como inteiros, replicando bolsas reais.

## Destaques de Performance 

- **Latência em Memória:** ~2-7µs.
- **Latência de Ponta a Ponta (Rede, Match e Persistência):** ~30µs - 60µs para Liquidity Makers e ~70µs - 120µs para Liquidity Takers (após JVM Warm-Up, a primeira ordem Maker leva até 1ms e a primeira Taker leva até 4ms).
- **Throughput:** Escalável para milhares de Operações por Segundo (OPS) sem contenção de threads.



## Arquitetura do Sistema

O projeto foi construído seguindo princípios avançados de High Frequency Trading (HFT), dividido em quatro camadas principais:

### 1. Engine Principal (Single-Writer Principle)
O coração do motor opera em uma thread dedicada e isolada, eliminando a necessidade de Locks.
- **Ring Buffer Lock-Free:** Mensageiros de rede (Data Transfer Objects - DTOs) chegam e são enfileirados em um Ring Buffer usando operações atômicas.
- **Object Pool (Zero Garbage Collection):** Uma object pool independente gerencia a memória das ordens no livro, eliminando a atuação do *Garbage Collector*.
- **Price-Time Priority:** Utiliza `TreeMap` e `LinkedList` para busca do melhor preço em $O(1)$ e inserção em $O(\log n)$.

### 2. Networking (Sockets TCP)
O servidor utiliza um Thread Pool (`ExecutorService`) para poder gerenciar múltiplas conexões simultaneamente. O protocolo de comunicação é via TCP, permitindo o envio simultâneo de ordens de diversos clientes.

### 3. Batch Processing e Persistência Lock-Free
Para evitar que as operações de I/O no disco rígido prejudiquem a latência das execuções na RAM, o sistema isola o banco de dados através do padrão Producer-Consumer:
- A Engine envia eventos de salvamento (novas ordens, modificações, cancelamentos, trades) para um segundo **Ring Buffer Lock-Free** baseado em arrays primitivos, transportando `Enums` de forma nativa e eliminando o impacto do *Garbage Collector* gerado por filas padrão do Java.
- Uma thread dedicada (`PersistenceWorker`) consome esses arrays e utiliza **Batch Processing** via API JDBC para aglomerar transações no SQLite em um único lote, absorvendo picos de volume de instruções sem prejudicar a latência.
- Em caso de reinicialização, o sistema faz a reconstrução do banco de dados lendo os atributos exatos de persistência, realimentando a memória RAM e reativando os rastreadores das ordens Pegged.

### 4. Telemetria e Monitoramento
O sistema auto-monitora sua integridade através de:
- **Percentis de Latência:** Cálculo de p50, p95 e p99 para identificar jitter e desvios de latência.
- **OPS Tracking:** Monitoramento em tempo real da vazão de ordens por segundo.



## Tecnologias Utilizadas

- **Linguagem:** Java 17+
- **Banco de Dados:** SQLite
- **Concorrência:** Ring Buffer, AtomicLong, Object Pooling e Thread Pools (sem uso de Locks bloqueantes).
- **Rede:** Java Sockets (TCP)

---

## Protocolo de Mensagens

As ordens devem ser enviadas ao servidor seguindo o formato separado por espaço:
`limit SIDE PRICE QUANTITY`, caso seja uma ordem limit, `market SIDE QUANTITY` caso seja uma ordem market ou `peg REFERENCE SIDE QUANTITY` caso seja uma ordem pegged

Exemplos de payloads válidos: 
 >`limit buy 30 100`\
 >`market sell 50`\
 >`peg bid buy 200`\
 >`peg offer sell 30`

## Ciclo de Vida e Fluxo de uma Ordem (Memory Management)

Para garantir máxima performance sem acionar o Garbage Collector, o ciclo de vida das ordens opera em duas trilhas de memória separadas:

1. **Ingresso e DTO:** A ordem (string) chega via Socket TCP, é tratada pelo `TradingServer` e os parâmetros brutos são atribuídos a um mensageiro (DTO `OrderCommand`) pré-alocado no `Ring Buffer`.
2. **Transferência (Object Pool):** A thread da `MatchingEngine` lê o DTO, pega emprestada uma entidade real do **Object Pool** de 100.000 instâncias via `borrowOrder()`, e a limpa injetando os novos dados através do método `reset()`.
3. **Reciclagem dos DTOs:** O DTO no Ring Buffer perde utilidade imediatamente após a leitura e será sobrescrito sem Garbage Collection quando o ponteiro de rede fizer o wrap-around.
4. **Matching:** A entidade `Order` real é cruzada instantaneamente no `OrderBook` na memória RAM e envia instruções assíncronas para o `PersistenceWorker`, e a gravação é feita em lotes no SQLite preservando os tipos originais.
5. **Repouso no Livro ou Reciclagem:** Se a ordem possuir saldo (Maker), ela repousa passivamente no livro. Assim que for 100% preenchida ou cancelada, ela é removida da árvore de preços e devolvida à pool via `returnOrder()`, ficando pronta para a próxima conexão de rede sem gerar lixo.



## Histórico de Redução de Latência Ponta a Ponta

(Até a 4a versão, testes foram realizados somente com ordens limit Liquidity Makers no computador local, resultado pode variar de acordo com a máquina)

1. 1a versão (síncrona):
   Cerca de 30ms.
   
2. 2a versão (Implementação de Producer-Consumer e gravação no banco de dados em background):
   Cerca de 800µs em inputs isolados.   
   
3. 3a versão (Adição de Single Writer-Principle, Ring Buffer e Batch Processing):
   Cerca de 45µs a 60µs em inputs isolados.

4. 4a versão (Implementação de um segundo ring buffer para escrita assíncrona no banco de dados):
   Cerca de 30 a 50µs para inputs isolados e 15µs a 30µs com a atuação máxima da Tiered Compilation.
   
5. Versão final (Adição de Object Pool, Pegged Orders e cumprimento total dos requisitos): 
   Latência base inalterada com a adição das lógicas dinâmicas de rastreamento de preço e separação de DTOs.
   
   - Para *Liquidity Makers* (Limit passivas ou Pegged): 
     ~30 a 60µs (isoladas) e ~15 a 30µs após o warm-up da Tiered Compilation.
   
   - Para *Liquidity Takers* (Market ou Limit agressivas que cruzam o spread):
     ~70 a 120µs (isoladas) e ~40 a 70µs após o warm-up da Tiered Compilation.


## Instruções para Utilização

Este projeto utiliza o **Maven** para o gerenciamento de dependências. Não é necessária a instalação manual de bibliotecas externas (o driver do SQLite será baixado automaticamente). 

Você pode executar o projeto de duas maneiras: através da sua IDE ou via Terminal.

### Opção A: Execução via IDE
1. Importe a pasta do projeto na sua IDE como um projeto Maven.
2. Execute o método principal da classe servidora: `src/main/java/app/Main.java`.
3. Em seguida, execute a classe cliente para poder interagir e enviar ordens: `src/main/java/network/TradingClient.java` (pode ser executada em múltiplas instâncias).

### Opção B: Execução via Terminal (Maven)
Certifique-se de ter o Maven instalado e execute os comandos na raiz do projeto:

1. **Compilar o projeto:**
   ```bash
   mvn clean compile

2. **Iniciar o Servidor:**
   ```bash
   mvn exec:java -Dexec.mainClass="app.Main"

3. **Iniciar o Cliente (em um novo terminal):**
   ```bash
   mvn exec:java -Dexec.mainClass="network.TradingClient"
---

Autor: [Daniel Kuhn](https://github.com/DanielUmedaKuhn)
