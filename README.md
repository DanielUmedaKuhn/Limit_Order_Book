# Limit Order Book em Java (Arquitetura High Frequency Trading)

Este projeto consiste em um motor de negociação (Matching Engine) de ultra-baixa latência capaz de processar milhares de orders por segundo. O sistema implementa um Limit Order Book completo, com arquitetura Lock-Free, persistência assíncrona em lote, conectividade via rede e telemetria em tempo real.

## Destaques de Performance 📈

* **Latência em Memória:** ~2-7µs.
* **Latência de Ponta a Ponta (Rede, Match e Persistência):** ~50µs a 100µs (após JVM Warm-Up, que leva cerca de 1ms após algumas poucas orders).
* **Throughput:** Escalável para milhares de Operações por Segundo (OPS) sem contenção de threads.

---

## Arquitetura do Sistema

O projeto foi construído seguindo princípios avançados de High Frequency Trading (HFT), dividido em quatro camadas principais:

### 1. Engine Principal (Single-Writer Principle)
O coração do motor opera em uma thread dedicada e isolada, eliminando a necessidade de Locks.
* **Ring Buffer Lock-Free:** As orders chegam das threads de rede e são enfileiradas em um Ring Buffer usando operações atômicas, garantindo que nunca precise esperar nenhuma operação.
* **Price-Time Priority:** Utiliza `TreeMap` e `LinkedList` para busca do melhor preço em $O(1)$ e inserção em $O(\log n)$.

### 2. Networking (Sockets TCP)
O servidor utiliza um Thread Pool (`ExecutorService`) para poder gerenciar múltiplas conexões simultaneamente. O protocolo de comunicação é via TCP, permitindo o envio simultâneo de orders de diversos clientes.

### 3. Batch Processing
Para evitar que o disco rígido atrase as trades, o sistema utiliza o padrão Producer-Consumer:
* O motor joga tarefas de salvamento para uma `BlockingQueue` em memória.
* O `PersistenceWorker` dedicado drena essa fila e utiliza a **Stream API** e **Batch Processing** (`executeBatch()`) para gravar múltiplas orders e trades no SQLite em uma única viagem de rede, mantendo a latência num valor mínimo.

### 4. Telemetria e Monitoramento
O sistema auto-monitora sua integridade através de:
* **Percentis de Latência:** Cálculo de p50, p95 e p99 para identificar jitter e desvios de latência.
* **OPS Tracking:** Monitoramento em tempo real da vazão de orders por segundo.

---

## Tecnologias Utilizadas

* **Linguagem:** Java 17+
* **Banco de Dados:** SQLite
* **Concorrência:** Ring Buffer, AtomicLong, BlockingQueues e Thread Pools (Sem uso de Locks bloqueantes).
* **Rede:** Java Sockets (TCP)

---

## Protocolo de Mensagens

As orders devem ser enviadas ao servidor seguindo o formato delimitado por ponto e vírgula:
`LADO;PREÇO;QUANTIDADE;TIPO`

| Campo | Valores Possíveis | Exemplo |
| :--- | :--- | :--- |
| **LADO** | `BUY`, `SELL` | `BUY` |
| **PREÇO** | Inteiro longo (`long`) | `30` |
| **QUANTIDADE** | Inteiro (`int`) | `100` |
| **TIPO** | `LIMIT`, `MARKET` | `LIMIT` |

> Exemplo de payload válido: `BUY;30;100;LIMIT`

---

## Instruções para Utilização

### Passo 1: Iniciar o Servidor
Execute o método principal da classe `app.Main`. O console indicará:
1. Criação das tabelas no arquivo `trading_system.db`.
2. Execução da rotina `rebuildBookFromDatabase` para recuperar o estado do mercado.
3. Inicialização das threads de Persistência e da Engine principal.
4. Abertura da porta TCP 8080.

### Passo 2: Iniciar o Cliente
Execute a classe `network.TradingClient` (pode ser executada em múltiplas instâncias). O terminal abrirá um prompt para envio das orders.

---

## Fluxo de uma Order

1. **Ingresso:** A string chega via Socket TCP e é tratada pelo `TradingServer`.
2. **Enfileiramento das Orders:** A order é inserida no `Ring Buffer` através de uma operação atômica de incremento rápido.
3. **Matching:** A thread isolada do `MatchingEngine` consome a order do Ring Buffer e executa o cruzamento instantâneo contra o `OrderBook` na memória RAM.
4. **Fase de Persistência:** Os novos saldos e trades gerados são enviados para a fila do `PersistenceWorker`.
5. **Gravação em Lote:** O worker drena a fila e executa os comandos no SQLite em lote.
6. **Egresso:** O cliente recebe a resposta textual contendo o ID da ordem.

---

## Histórico de Redução de Latência Ponta a Ponta 

(Testados no meu computador pessoal, resultado pode variar de acordo com a máquina)

1. 1a versão (síncrona):
   Cerca de 30ms.
   
2. 2a versão (Implementação de Producer-Consumer e gravação no banco de dados em background):
   Cerca de 800µs.
   
3. Versão final (Adição de Single Write-Principle, Ring Buffer e Batch Processing):
   Cerca de 60µs.

---

Autor: [Daniel Kuhn](https://github.com/DanielUmedaKuhn)
