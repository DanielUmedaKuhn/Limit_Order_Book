# High-Performance Matching Engine in Java

Este projeto consiste em um motor de negociação (Matching Engine) de baixa latência capaz de processar milhares de ordens por segundo. O sistema implementa um Limit Order Book (LOB) completo, com persistência assíncrona, conectividade via rede e telemetria em tempo real.

## Destaques de Performance

* **Latência em Memória:** ~2-7µs.
* **Latência de Ponta a Ponta (Rede, Match e Persistência):** ~800µs.
* **Throughput:** Escalável para milhares de Operações por Segundo (OPS).

---

## Arquitetura do Sistema

O projeto foi construído seguindo princípios de sistemas distribuídos e alta disponibilidade, dividido em quatro camadas principais:

### 1. Core Engine 
Utiliza uma estrutura de dados baseada em `TreeMap` e `LinkedList` para garantir prioridade de Preço e Tempo (Price-Time Priority).
* **Bids (Compra):** Ordenados do maior para o menor preço.
* **Asks (Venda):** Ordenados do menor para o maior preço.
* **Complexidade Algorítmica:** Busca de melhor preço em $O(1)$ e inserção em $O(\log n)$.

### 2. Networking (Sockets TCP)
O servidor utiliza um Thread Pool (`ExecutorService`) para gerenciar múltiplas conexões simultâneas de clientes. O protocolo de comunicação é baseado em texto comum via TCP, permitindo que qualquer terminal conectado envie ordens de forma independente.

### 3. Persistência Assíncrona 
Para evitar o gargalo do disco rígido (I/O), foi implementado o padrão Producer-Consumer com o uso de uma `BlockingQueue`.
* O motor (Producer) envia tarefas de banco para a fila.
* Um `PersistenceWorker` (Consumer) dedicado processa as gravações no SQLite em background, liberando o motor para o próximo match instantaneamente na memória RAM.

### 4. Telemetria e Monitoramento
O sistema auto-monitora sua integridade através de:
* **Percentis de Latência:** Cálculo de p50, p95 e p99 para identificar jitter e desvios de latência induzidos por Garbage Collection.
* **OPS Tracking:** Monitoramento em tempo real da vazão de ordens por segundo no relatório de telemetria.

---

## Tecnologias Utilizadas

* **Linguagem:** Java 17+
* **Banco de Dados:** SQLite (Persistência de Ordens e Trades)
* **Concorrência:** ReentrantLocks, LongAdders, BlockingQueues e Thread Pools (ExecutorService)
* **Rede:** Java Sockets (TCP)

---

## Protocolo de Mensagens

As ordens devem ser enviadas ao servidor seguindo o formato delimitado por ponto e vírgula:
`LADO;PREÇO;QUANTIDADE;TIPO`

| Campo | Valores Possíveis | Exemplo |
| :--- | :--- | :--- |
| **LADO** | `BUY`, `SELL` | `BUY` |
| **PREÇO** | Inteiro longo (`long`) | `150` |
| **QUANTIDADE** | Inteiro (`int`) | `10` |
| **TIPO** | `LIMIT`, `MARKET` | `LIMIT` |

> Exemplo de payload válido: `BUY;10500;50;LIMIT`

---

## Instruções para Utilização

### Pré-requisitos
* Java Development Kit (JDK) 17 ou superior instalado.
* IDE (Utilizado IntelliJ IDEA) ou terminal com acesso ao compilador `javac`.

### Passo 1: Iniciar o Servidor
Execute o método principal da classe `app.Main`. O console indicará o ciclo de inicialização:
1. Verificação e criação das tabelas no arquivo `trading_system.db`.
2. Execução da rotina `rebuildBookFromDatabase` para recuperar ordens abertas e remontar o estado anterior do livro na RAM.
3. Abertura e escuta na porta TCP 8080.

### Passo 2: Iniciar o Cliente
Execute o método principal da classe `network.TradingClient` (pode ser executado em múltiplas instâncias para simular concorrência). O terminal do cliente abrirá um prompt para envio de mensagens baseadas no protocolo.

### Passo 3: Monitoramento de Métricas
A cada 10 segundos, o console do Servidor exibirá automaticamente o relatório de telemetria contendo o OPS atualizado, a quantidade total de ordens/trades e os percentis calculados.

---

## Fluxo de uma Ordem 

1. **Ingresso:** A string de rede chega via Socket TCP e é tratada pelo `TradingServer`.
2. **Persistência (Fase 1):** O comando de inserção (`SAVE_ORDER`) é enviado de forma não-bloqueante para o `PersistenceWorker`.
3. **Matching:** O `MatchingEngine` intercepta a ordem sob escopo de exclusão mútua (`ReentrantLock`) e executa o cruzamento contra o `OrderBook`.
4. **Execução:** Se houver correspondência de preço, um objeto `Trade` é gerado e os saldos das ordens passivas e agressoras são alterados na RAM.
5. **Persistência (Fase 2):** As atualizações de saldo e o histórico do negócio são enviados para a fila do `PersistenceWorker`.
6. **Egresso:** O cliente recebe a resposta textual contendo o ID da ordem gerada e a quantidade de matches ocorridos.
