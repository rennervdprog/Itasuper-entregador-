# Continuidade completa — ItaSuper Entregador

**Data de consolidação:** 22 de agosto de 2026
**Aplicativo:** ItaSuper Entregador
**Pacote Android:** `app.itasuper.parceiro`
**Repositório:** [rennervdprog/Itasuper-entregador-](https://github.com/rennervdprog/Itasuper-entregador-)
**Branch de trabalho/publicada:** `main`
**Estado do repositório nesta consolidação:** limpo e sincronizado com `origin/main`
**Objetivo deste documento:** permitir que uma nova conversa retome o trabalho exatamente do ponto atual, caso o contexto desta conversa seja apagado após a reinicialização.

> **Regra de segurança:** este documento não contém senhas, Base64, chaves privadas, URLs privadas, tokens, valores de segredos ou dados pessoais. As credenciais de assinatura foram entregues separadamente ao usuário e já foram cadastradas no GitHub.

---

## 1. Resumo executivo do ponto atual

O aplicativo nativo **ItaSuper Entregador** está em Kotlin com Jetpack Compose, ligado ao Supabase e destinado **exclusivamente a motoboys vinculados a lojas**. Não deve adquirir recursos de cliente, lojista, administrador, catálogo, carrinho, checkout, saque de plataforma ou qualquer fluxo que descaracterize o aplicativo de entregador de loja.

O trabalho funcional relevante já publicado na `main` cobre identidade visual, sessão persistente, biometria, presença do entregador, diretório voluntário por município, convites de loja, sincronização automática de pedidos via Realtime, recuperação de pedidos após retorno do segundo plano e sanitização de mensagens técnicas para o entregador. O pipeline de release assinado também foi concluído, publicado e validado no GitHub Actions.

A última execução de release foi bem-sucedida. Ela compilou, assinou, verificou e publicou um AAB e um APK de validação. Essa execução não alterou o banco, regras de negócio, RLS, migrations, Edge Functions nem publicou o aplicativo na Google Play.

| Item | Estado confirmado |
|---|---|
| Código em `main` | Publicado e sem alterações locais pendentes |
| Release assinada via GitHub Actions | Configurada, manual e validada |
| Chave de upload | Exclusiva do aplicativo de entregador; não é a chave do app cliente |
| Segredos do GitHub Actions | Configurados e aprovados na execução final |
| AAB assinado | Gerado como artefato de validação |
| APK assinado | Gerado como artefato de validação |
| Banco/Supabase/RLS | Não alterados durante o trabalho de release |
| Próxima ação obrigatória | Não há correção técnica bloqueante; preservar backups e, quando desejado, definir a próxima versão funcional ou de Play Store |

---

## 2. Identidade técnica do projeto

| Campo | Valor |
|---|---|
| Linguagem | Kotlin |
| UI | Jetpack Compose / Material 3 |
| Namespace | `com.example` |
| `applicationId` | `app.itasuper.parceiro` |
| `minSdk` | 24 |
| `targetSdk` | 36 |
| `compileSdk` | 36 (minor API level 1) |
| Versão atual | `versionCode 38` / `versionName 2.3.6-recuperacao-segundo-plano` |
| Backend | Supabase, com URL pública e chave Publishable/Anon fornecidas em configuração local/segura |
| Repositório remoto | `https://github.com/rennervdprog/Itasuper-entregador-.git` |
| Branch de produção técnica deste repositório | `main` |

A versão não foi incrementada exclusivamente para testar o pipeline. Antes de uma atualização real na Play Store, confirmar se o `versionCode` precisa subir para não conflitar com uma versão já enviada à mesma ficha da Play Store.

---

## 3. Limites de produto que devem ser preservados

O aplicativo é do **motoboy de loja**. Toda evolução futura precisa manter os limites abaixo.

| Permitido | Não permitido neste app |
|---|---|
| Login, sessão, biometria e cadastro de entregador | Login/checkout de cliente |
| Vínculo do motoboy a lojas e convites | Área de administração de loja |
| Disponibilidade online/offline e presença | Painel financeiro de plataforma |
| Pedidos disponíveis, aceite, rota e confirmação | Catálogo, carrinho e pagamentos do cliente |
| Navegação para Maps/Waze | Saques de entregador de plataforma |
| Histórico, suporte e perfil | Recursos exclusivos de lojista ou admin |
| Base voluntária de entregadores por cidade | Vínculo automático de qualquer entregador à loja |

As regras de negócio também devem continuar preservadas:

1. `store_drivers` é a fonte de verdade para vínculo entre loja e entregador.
2. Convites `pending` ou `rejected` não podem entrar em pedidos, rotas, presença, disponibilidade, Realtime ou regras operacionais.
3. A base `driver_directory_preferences` é opcional e voluntária; ela facilita descoberta por município, mas não cria vínculo automático com loja.
4. As regras VIP e a separação entre presença e pedidos não devem ser alteradas por mudanças no app do entregador.
5. A taxa de entrega e a taxa de plataforma são definidas pelo fluxo web/lojista; o entregador não deve receber regras inventadas de preço.

---

## 4. Funcionalidades já implementadas e publicadas

### 4.1 Sessão, login, biometria e identidade visual

O aplicativo recebeu melhorias de sessão persistente e biometria. O comportamento esperado é manter o entregador logado quando a sessão Supabase ainda for válida, oferecer entrada por biometria quando o usuário a tiver habilitado e impedir que o botão de voltar revele uma área autenticada depois de sair da conta.

A identidade visual foi atualizada para a linguagem do **capacete com raio** do ItaSuper Entregador, incluindo ajustes de splash e ícones. O objetivo é manter os ativos de entregador consistentes e distintos do aplicativo cliente.

O commit principal desta etapa é `4c99842` (`feat: improve driver presence biometric and icon identity`).

### 4.2 Presença do entregador

A presença é adaptativa para evitar tráfego agressivo e comportamento incorreto em segundo plano. O heartbeat foi definido para aproximadamente **8 minutos**, somente quando o entregador está online, livre e o aplicativo está em primeiro plano. O backend foi alinhado para tolerar até **13 minutos** antes de considerar a presença expirada. Isso reduz chamadas desnecessárias e evita depender de atualização contínua quando o entregador estiver, por exemplo, navegando no Waze ou Maps.

A disponibilidade é separada da lista de pedidos. Ficar offline deve refletir corretamente a indisponibilidade do entregador para a loja, sem alterar pedidos ou regras de vínculo.

### 4.3 Pedidos, Realtime, segundo plano e rotas

Foram tratados sincronização inicial, atualização automática e recuperação ao retornar do segundo plano. O aplicativo deve se inscrever novamente no Realtime e recarregar pedidos quando necessário, sem exigir que o entregador pressione manualmente “Atualizar pedidos”.

A filtragem de elegibilidade é estrita: apenas lojas cujo vínculo do entregador esteja aceito podem alimentar os pedidos e a operação. Convites pendentes e recusados ficam fora do fluxo.

Há suporte aos fluxos de entregas disponíveis, aceite, rota ativa, próxima parada, navegação externa para GPS, confirmação e histórico. A lógica operacional discutida para múltiplas entregas deve ser mantida: se o entregador aceitou as entregas disponíveis que formam sua rota ativa, novas entregas não devem ser aceitas até a finalização dessa rota, conforme a regra funcional definida pelo usuário.

O commit principal desta correção é `238f4d7` (`fix: recover order sync and sanitize driver errors`).

### 4.4 Convites e diretório por cidade

O Perfil mostra os convites pendentes de loja. Aceite e recusa são condicionais ao próprio `driver_user_id` e ao status `pending`; recusa atualiza o estado para `rejected`, sem apagar registros de forma indevida.

O diretório de entregadores por município é uma adesão voluntária. O selecionador de cidade foi trabalhado para evitar registros imprecisos, melhorar legibilidade e não converter uma preferência de cidade em vínculo automático com qualquer loja.

O commit principal dessa etapa é `3830dde` (`fix: sync driver orders automatically and show pending invites`).

### 4.5 Experiência operacional, mensagens e telas

O aplicativo recebeu melhorias de operação e fluxo: cadastro real de entregador baseado no comportamento do Capacitor/web, tela de chegada/alerta de entregas, histórico, informações operacionais do pedido, formas de pagamento como Pix direto, cartão/maquininha e dinheiro/troco, além de descrição de itens no cartão de entrega.

As mensagens exibidas ao entregador foram centralizadas para não mostrar termos técnicos de Android, Supabase, HTTP, tabelas ou exceções internas. O arquivo central é:

```text
app/src/main/java/com/example/ui/common/DriverUserMessage.kt
```

Qualquer mensagem nova deve continuar sendo convertida para texto simples e acionável para o motoboy.

### 4.6 Notificações e alerta sobreposto

Foram implementados fluxos de notificação e alerta de entrega disponível, incluindo pedido de permissões e uma tela/card de sobreposição em tela cheia quando aplicável. O design foi retrabalhado para ser mais rico em informações do pedido, com número no formato do site, dados de pagamento, itens e demais detalhes operacionais relevantes.

Em futuras mudanças, não introduzir serviços agressivos de background ou GPS contínuo sem necessidade. O objetivo é notificar corretamente, economizar bateria e não aumentar requisições ao Supabase desnecessariamente.

---

## 5. Arquivos relevantes no código

| Arquivo | Responsabilidade principal |
|---|---|
| `app/build.gradle.kts` | Configuração Android, versão, leitura de configurações públicas e assinatura obrigatória de release |
| `.github/workflows/android-release.yml` | Pipeline manual para AAB/APK assinados |
| `.github/workflows/build-debug-apk.yml` | Build debug existente para APK de teste |
| `.gitignore` | Ignora `*.jks`, `*.keystore`, `keystore.properties` e configurações locais sensíveis |
| `app/src/main/java/com/example/data/remote/SupabaseDriverOrdersRepository.kt` | Pedidos, sincronização inicial, Realtime, recuperação e filtros de loja aceita |
| `app/src/main/java/com/example/data/remote/SupabaseDriverLinkRepository.kt` | Convites de loja, aceite e recusa seguros |
| `app/src/main/java/com/example/ui/common/DriverUserMessage.kt` | Sanitização de erros para linguagem do entregador |
| `app/src/test/java/com/example/GreetingScreenshotTest.kt` | Contém uma falha preexistente a ser corrigida apenas com autorização específica |

---

## 6. Histórico de commits publicado

| Commit | Mensagem | Estado/escopo |
|---|---|---|
| `f21f090` | `ci: relax AAB signature verification strictness` | Ajusta verificação do AAB para compatibilidade, preservando verificação criptográfica |
| `4d2e643` | `ci: add signed Android release workflow` | Cria workflow manual de AAB/APK assinados e endurece assinatura release |
| `238f4d7` | `fix: recover order sync and sanitize driver errors` | Recuperação de pedidos no retorno do segundo plano e mensagens amigáveis |
| `3830dde` | `fix: sync driver orders automatically and show pending invites` | Sincronização inicial/Realtime, convites no Perfil e municípios |
| `4c99842` | `feat: improve driver presence biometric and icon identity` | Presença, biometria, sessão e identidade visual |
| `7a45ade` | `fix: prevent startup crash without Supabase config` | Evita encerramento abrupto se a configuração pública estiver ausente |
| `816b8ec` | `feat: improve driver operations, history and signup` | Melhorias em operações, histórico e cadastro de entregador |
| `bdc3e65` | `ci: build debug apk artifact` | Pipeline de APK debug |
| `a49cf9b` | `feat: complete driver delivery flows and arrival alerts` | Fluxos de entrega, chegada e alertas |
| `81d8acd` | `feat: initialize project structure` | Estrutura inicial do projeto |

Nenhuma credencial foi incluída em nenhum desses commits.

---

## 7. Pipeline de release assinado — estado final

### 7.1 Princípios de segurança aplicados

A assinatura release não usa fallback para chave debug, senha padrão, alias fixo ou arquivo local rastreável. Ao chamar uma tarefa release sem todos os valores necessários, o Gradle falha explicitamente com mensagem segura.

O `app/build.gradle.kts` exige, para tarefas release:

| Variável de build | Finalidade |
|---|---|
| `KEYSTORE_PATH` | Caminho temporário para a chave de upload restaurada no runner |
| `STORE_PASSWORD` | Senha do keystore |
| `KEY_ALIAS` | Alias da chave de upload |
| `KEY_PASSWORD` | Senha do alias |

O workflow usa exclusivamente seis segredos de repositório no GitHub Actions:

| Nome do segredo no GitHub | Uso |
|---|---|
| `ANDROID_UPLOAD_KEYSTORE_BASE64` | Keystore `.jks` codificado em Base64 |
| `ANDROID_KEYSTORE_PASSWORD` | Senha do keystore |
| `ANDROID_KEY_ALIAS` | Alias da chave exclusiva do entregador |
| `ANDROID_KEY_PASSWORD` | Senha do alias |
| `SUPABASE_URL` | URL pública do projeto Supabase, no formato `https://...supabase.co` |
| `SUPABASE_PUBLISHABLE_KEY` | Chave Publishable/Anon pública do mesmo projeto |

> Nunca substituir `SUPABASE_PUBLISHABLE_KEY` por `service_role`, senha do PostgreSQL, URI direta do banco ou token administrativo. Nunca colocar qualquer um desses valores em código, `.yml`, commit, issue, chat público ou log.

### 7.2 Funcionamento do workflow

O workflow se chama **Android Release Bundle** e está ativo no GitHub. Ele é disparado somente manualmente por `workflow_dispatch`.

| Etapa | Comportamento |
|---|---|
| Validação do rótulo | Aceita apenas letras, números, ponto, hífen e sublinhado em `release_label` |
| Configuração pública | Cria `local.properties` temporário com as duas configurações públicas Supabase |
| Validação de segredos | Interrompe a execução se qualquer segredo de assinatura estiver vazio |
| Restauração da chave | Reconstrói o `.jks` em `$RUNNER_TEMP`, usa permissões `600` e não grava no Git |
| Build | Executa `:app:bundleRelease` e `:app:assembleRelease` |
| Verificação | Usa `jarsigner -verify -certs` no AAB e `apksigner verify --verbose` no APK |
| Coleta | Nomeia AAB/APK com `versionName`, `release_label` e número da execução |
| Upload | Publica AAB e APK como artifacts por 14 dias |
| Limpeza | Remove o keystore temporário e `local.properties`, inclusive em falha |

A alteração `f21f090` removeu somente a opção `-strict` do `jarsigner` na validação do AAB. O `-strict` reprovava avisos de estrutura interna do formato AAB apesar de a assinatura estar correta. A verificação criptográfica do AAB continua ativa com `jarsigner -verify -certs`, e a validação detalhada do APK continua ativa com `apksigner verify --verbose`.

### 7.3 Validações realizadas

| Validação | Resultado |
|---|---|
| `assembleRelease` sem segredos | Falhou corretamente, antes de assinar, por falta de variável segura |
| Build local isolado com chave temporária | AAB e APK gerados e verificados; a chave temporária foi removida |
| Primeira validação no GitHub | Parou com segurança porque `SUPABASE_URL` e `SUPABASE_PUBLISHABLE_KEY` ainda não estavam cadastrados |
| Segunda validação no GitHub | Configuração pública e segredos de assinatura aprovados; build concluído; `jarsigner -strict` reprovou aviso estrutural do AAB |
| Correção do verificador | Publicada no commit `f21f090` |
| Validação final no GitHub | Concluída com sucesso, incluindo AAB, APK, verificação e uploads |

A mensagem de anotação `403` exibida pela ferramenta de acompanhamento do GitHub sobre leitura de annotations é uma limitação do token de integração para consultar anotações de checks. Ela **não** afetou o workflow; a execução final foi concluída como `success`.

---

## 8. Última execução de release aprovada

| Campo | Valor |
|---|---|
| Workflow | `Android Release Bundle` |
| Execução | `32574419704` |
| Status | `success` |
| Commit usado | `f21f090fa0096eafcb784c7f5e8ee799d216990b` |
| Rótulo de validação | `validacao-final` |
| Página da execução | [GitHub Actions — validação final](https://github.com/rennervdprog/Itasuper-entregador-/actions/runs/32574419704) |
| Retenção de artifacts | 14 dias |

Os artefatos presentes nessa execução são:

| Nome do artifact | Tamanho aproximado | Finalidade |
|---|---:|---|
| `itasuper-entregador-aab-validacao-final-3` | 18,35 MB | Arquivo oficial para envio à Google Play Store |
| `itasuper-entregador-apk-validacao-final-3` | 18,25 MB | Instalação interna/homologação |

Esses artifacts representam uma **validação de build assinada**. Eles não significam que o aplicativo foi enviado ou publicado na Play Store. Para uma release real de Play Store, revisar versão, alterar `versionCode` quando necessário, gerar nova build e então fazer o envio pelo Play Console.

---

## 9. Credenciais de assinatura — situação e cuidados

Foi criada uma chave JKS exclusiva do app entregador, com o alias:

```text
itasuper_entregador_upload
```

A chave é exclusiva do pacote `app.itasuper.parceiro`. Não usar, copiar ou substituir pela chave do aplicativo cliente.

O usuário recebeu um pacote privado contendo:

| Arquivo originalmente entregue | Finalidade |
|---|---|
| `itasuper-entregador-upload-key.jks` | Backup crítico da chave privada de upload |
| `ANDROID_UPLOAD_KEYSTORE_BASE64.txt` | Valor para o segredo GitHub de mesmo nome |
| `ANDROID_KEYSTORE_PASSWORD.txt` | Valor para o segredo GitHub de mesmo nome |
| `ANDROID_KEY_ALIAS.txt` | Alias para o segredo GitHub de mesmo nome |
| `ANDROID_KEY_PASSWORD.txt` | Valor para o segredo GitHub de mesmo nome |
| `GUIA_DE_CADASTRO_GITHUB.md` | Passo a passo de cadastro no GitHub |
| `SHA256SUMS.txt` | Manifesto de integridade dos arquivos críticos |

A chave foi validada com `keytool`, o Base64 foi conferido contra a chave original e as credenciais não foram incluídas no repositório. O usuário também confirmou o cadastro no GitHub, e a execução final provou que os seis segredos estão utilizáveis.

> Guardar a JKS e as senhas em local privado, de preferência cofre de senhas e backup criptografado. Perder a JKS ou a senha de assinatura pode impedir atualizações futuras do mesmo aplicativo na Play Store. Não enviar a chave por WhatsApp, não anexar em repositório, não colocar em chat e não copiar para código.

Quando o usuário confirmar que possui backup fora deste ambiente, os arquivos temporários de credenciais que possam permanecer no ambiente de trabalho devem ser removidos de forma segura. Não removê-los antes dessa confirmação.

---

## 10. Pendência conhecida fora do escopo de release

O comando de teste unitário debug apresenta uma falha preexistente em:

```text
app/src/test/java/com/example/GreetingScreenshotTest.kt
```

A causa registrada é o uso de parâmetros que não existem mais:

```text
onNavigateToOnboarding
onNavigateToDashboard
```

Essa falha não bloqueou a compilação release e não foi corrigida porque a autorização atual era focada em pipeline e assinatura. Se o usuário autorizar, a próxima sessão pode corrigir esse teste em uma mudança isolada, rodar `:app:testDebugUnitTest` e publicar somente após apresentar o diff.

---

## 11. Regras de operação para a próxima sessão

1. Não criar commit, push, publicação de Play Store, alteração Supabase, mudança RLS, migration, Edge Function, tabela, segredo ou exclusão de arquivo sem autorização explícita do usuário.
2. Não imprimir, armazenar em documento, registrar em log ou publicar valores de senhas, Base64, URLs privadas, chaves, certificados, tokens ou `local.properties`.
3. Manter o aplicativo exclusivo de entregador de loja.
4. Preservar as regras de `store_drivers`, de convites pendentes/recusados e da base voluntária de cidade.
5. Não alterar a lógica de negócio de taxa, VIP, checkout ou cliente no app entregador sem análise e autorização específicas.
6. Em qualquer release futura, confirmar `versionCode`, `versionName`, `release_label`, segredos e finalidade da distribuição antes de executar o workflow.
7. Tratar AAB como arquivo oficial para Play Store e APK como material de homologação/instalação interna.
8. Antes de dizer que uma release está pronta, verificar o estado do workflow, a assinatura, os dois artifacts e a limpeza da chave temporária.

---

## 12. Próximos caminhos possíveis

Não há uma pendência técnica urgente no pipeline. A nova conversa deve perguntar qual das prioridades abaixo o usuário quer executar.

| Prioridade possível | Ação recomendada |
|---|---|
| Testar o APK final | Baixar `itasuper-entregador-apk-validacao-final-3`, instalar em dispositivo de homologação e validar login, pedidos, Realtime, convites, presença, alertas, rota e histórico |
| Preparar envio Play Store | Confirmar se a versão 38 já foi enviada; se necessário, atualizar `versionCode`/`versionName`, gerar release com rótulo de produção e revisar antes do Play Console |
| Corrigir teste unitário | Corrigir somente `GreetingScreenshotTest.kt`, rodar testes debug e publicar após autorização |
| Auditoria funcional/UI | Comparar novamente as telas nativas de entregador com o Capacitor/web, sem adicionar escopos de cliente/lojista/admin |
| Melhorar uma função específica | Reproduzir o cenário em dispositivo e analisar logs/dados reais sem alterar Supabase sem autorização |
| Revisar segurança operacional | Confirmar backups da JKS, segredos do GitHub e expiração dos artifacts; nunca exportar segredos ao chat |

---

## 13. Como reiniciar a conversa sem perder o contexto

Ao abrir uma nova conversa, anexar este arquivo e enviar a mensagem abaixo:

> Este é o projeto Android nativo **ItaSuper Entregador**, pacote `app.itasuper.parceiro`, exclusivo para motoboys de loja. Leia integralmente o arquivo de continuidade anexado e use-o como fonte de verdade do estado anterior. Não altere arquivos, Supabase, banco, RLS, migrations, Edge Functions, segredos, GitHub ou Play Store sem minha autorização explícita. Primeiro confirme o estado atual do repositório `rennervdprog/Itasuper-entregador-`, branch `main`, e pergunte qual prioridade devo executar: testar o APK, preparar uma release real, corrigir o teste unitário ou auditar o app.

Se a intenção for continuar no pipeline de release, acrescentar:

> Os seis segredos do GitHub Actions já foram validados em build com sucesso. Não me peça os valores e não os exponha. Use somente o workflow manual `Android Release Bundle` após confirmar `versionCode`, `versionName` e `release_label`.

---

## 14. Backup antes da reinicialização

Como há um aviso de backup na conta, o aviso recebido no aplicativo/e-mail é a fonte de verdade para saber se a conta está afetada. Para contas afetadas ou em dúvida, o backup de tarefas não é automático e deve ser feito pelo [Data Backup Tool](https://manus.im/backup) antes do prazo exibido no aviso. A orientação oficial indica como prazo geral **23 de agosto de 2026, às 7:59 a.m. SGT**.[1] [2]

O arquivo atual é um resumo de continuidade e não substitui o backup oficial. Para preservar o trabalho de forma robusta, guardar em local seguro:

| Material | Onde preservar |
|---|---|
| Este arquivo de continuidade | Computador, Drive privado ou cofre de documentos |
| Pacote JKS e senhas de release | Cofre de senhas e armazenamento criptografado; nunca Git ou chat público |
| AAB/APK da validação final | Baixar da página da execução GitHub antes do vencimento de 14 dias |
| Código-fonte | Já existe no GitHub na branch `main`; confirmar acesso à conta GitHub |
| Backup de dados Manus, se solicitado pelo aviso | Data Backup Tool oficial; manter todos os pacotes completos sem renomear ou misturar partes |

Todo backup oficial é uma fotografia do momento e não continua sincronizando. Para projetos com uso ativo, realizar o backup solicitado e, se houver novos trabalhos depois, considerar uma exportação final atualizada antes do prazo. A restauração de dados de tarefas deve ser planejada com atenção porque o processo de restauração pode ser realizado uma única vez.[1] [2]

---

## Referências

[1] [How to Back Up Your Data — Manus Help Center](https://help.manus.im/en/articles/16147892-service-change-overview-how-to-back-up-your-data)
[2] [Data Backup and Restoration — Manus Help Center](https://help.manus.im/en/collections/19704025-data-back-up-and-restoration)
[3] [Android Release Bundle — última execução validada](https://github.com/rennervdprog/Itasuper-entregador-/actions/runs/32574419704)
