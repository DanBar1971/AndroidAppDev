# SaúdePrática — Regra obrigatória de notas de versão

A cada nova versão da SaúdePrática, entregar **sempre**, juntamente com o APK assinado e o ZIP opcional, um texto pronto a copiar e partilhar com as pessoas que testam a aplicação.

## O que cada entrega deve incluir

1. **Versão exata** e, se fizer sentido, data da atualização.
2. **O que mudou**: novas funcionalidades e erros corrigidos, em linguagem simples.
3. **Como testar**: passos curtos para confirmar a principal alteração.
4. **Instalação**: atualizar por cima da versão anterior, sem desinstalar, para preservar os dados (desde que a assinatura e o package ID tenham sido verificados).
5. **Limitações conhecidas**: distinguir o que compilou do que foi efetivamente testado num telemóvel.
6. **Download**: ligação ao APK assinado e ao ZIP apenas quando os ficheiros exatos tiverem sido gerados e confirmados no ambiente atual.

Tom: português de Portugal, claro, objetivo, simpático. Formato: mensagem curta pronta para WhatsApp, email ou grupo de testers. Evitar termos técnicos que os testers não precisam de conhecer.

Não declarar uma correção como validada no telemóvel apenas porque o código compilou. Não prometer que uma funcionalidade funciona sem testes reais. Nunca sugerir desinstalar a aplicação para instalar uma atualização normal.

## Mensagem de exemplo — v1.9.39

**📱 SaúdePrática — Versão 1.9.39**

**Novidade:** a fotografia da caixa de medicamentos passa a usar uma câmara integrada na SaúdePrática, em vez da câmara externa do telemóvel. Foi concebida para corrigir o problema de fotografar, carregar no visto e não surgir nenhuma pergunta.

**Como testar:** Medicação → Adicionar → Fotografar caixa. Fotografar uma embalagem e confirmar se aparece a opção de analisar com ML Kit, guardar a imagem sem análise ou repetir a fotografia.

**Instalação:** atualizar por cima da versão anterior, sem desinstalar, para manter os dados.

**Nota:** esta versão compilou e foi assinada, mas a nova câmara ainda precisa de ser validada nos Xiaomi dos testers.

📎 **APK:** anexar o ficheiro do lançamento correspondente.
