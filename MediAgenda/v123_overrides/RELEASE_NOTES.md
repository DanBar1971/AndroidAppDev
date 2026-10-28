# SaúdePrática v1.9.103 — Hotfix de escolha de perfil e assistente no Android 16

## Correções urgentes
- **Escolher perfil:** nomes, fotografias e contadores de cada utente voltam a estar visíveis e o cartão do perfil pode ser selecionado por toque.
- **Associar alarmes a este telemóvel** passa a ser uma ação independente, numa linha inferior do cartão, sem tapar os nomes ou impedir a seleção.
- **Criar novo perfil:** botões **Anterior** e **Seguinte/Criar perfil** são mantidos acima da barra de navegação do Android 16/HyperOS 3, respeitando as margens reais do sistema e do teclado.
- O formulário mantém scroll independente e os botões finais ficam acessíveis quando o teclado está aberto.

## Mantido
- Corretivo v1.9.102 do alarme em ecrã bloqueado sem PIN da app, ADIAR 10M e TOMEI.
- Separação de TOMAS por perfil e contadores individuais.
- Opção de medicação **NÃO TEM CAIXA · Só lamelas/blisters**, imagens e bulas.
- Identificador da aplicação, dados guardados, ícone, splash e fundo originais.

## Verificações
- Testes automáticos do ecrã de escolha, original avatar/nome, associação independente, margens do Android e compilação Kotlin.
- Validar no Xiaomi Redmi Note 13 Pro / Android 16: selecionar os quatro perfis, associar os alarmes, abrir o novo assistente com teclado e carregar em Seguinte, sem elementos encobertos.
- Abertura automática do alarme em ecrã cheio depende das permissões especiais do HyperOS e continua a necessitar de teste no dispositivo real.
