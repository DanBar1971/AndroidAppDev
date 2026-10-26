# SaúdePrática — Instruções permanentes de continuidade (Projeto Android Apps)

## Pedido explícito do responsável (26-10-2026)
- Este repositório é a fonte de verdade da app SaúdePrática. **Ao iniciar qualquer novo chat neste mesmo Projeto, continuar o trabalho existente; não pedir novamente fontes, APKs, imagens ou chave de assinatura sem antes procurar aqui e nos ficheiros do Projeto/Library.**
- É responsabilidade de quem atualiza a app **compilar, validar, assinar e publicar cada nova versão no GitHub Releases**, não apenas preparar código ou descarregar uma APK para o chat. A publicação só está concluída depois de confirmar a tag, o estado Latest, o asset `SaudePratica.apk` e o funcionamento da ligação permanente.
- Não trocar package name, assinatura nem apagar dados existentes. Usar sempre versão crescente e manter changelog.
- Em caso de erro real após procurar o histórico, explicar precisamente a limitação, sem inventar publicação bem-sucedida.

## Localização dos elementos e histórico
- Repo GitHub: https://github.com/DanBar1971/AndroidAppDev
- Nome público: SaúdePrática. Código-fonte histórico em `MediAgenda/` e patches incrementais `MediAgenda/vNN_overrides/`; não eliminar esta pasta durante a migração.
- Workflow de compilação atual: `.github/workflows/mediagenda-v1969.yml`. Workflow de Release automática: `.github/workflows/saudepratica-publish-release.yml`.
- Procedimento completo: `SaúdePrática/PUBLICAR_APK_GITHUB.md`. Patches assinados: `SaúdePrática/release-patches/vX.Y.Z/`.
- **Chave original encontrada no histórico do Projeto/Library**: `MediAgenda-stable-signing-key.jks`; também `MediAgenda-Chave-Assinatura-PRIVADA.zip` e `MediAgenda-Chave-Assinatura-PRIVADA(1).zip` disponíveis na raiz da Library do utilizador (criados em 20-10-2026). Procurar com Files/Library, não apenas nos anexos da conversa atual. Nunca colocar keystore/passwords no repositório público ou repetir segredos em conversas.
- Já foi demonstrado que a mesma chave assinou a v1.9.79 compatível com a v1.9.78. Se a chave não estiver disponível no runner GitHub Actions, preparar uma APK assinada no ambiente privado e publicar por delta binário verificado (instruções abaixo).
- Gráficos originais e ZIPs de código/aplicações anteriores também existem em Library e no histórico. Procurar antes de perguntar.

## Publicação automática através do delta — fluxo já utilizado
1. Obter APK assinada da Release anterior `vX.Y.Z` e APK nova assinada na mesma chave. Verificar certificados Android e package `com.mediagenda.app.stable`, versão e integridade.
2. Calcular `sha256sum` da APK anterior, da APK nova e de `delta.zst`; gerar `zstd --patch-from=base.apk -7 new.apk -o delta.zst`.
3. Codificar delta em Base64, dividir em `delta.part00.b64`, `delta.part01.b64`, ... sem sobreposições e guardá-los em `SaúdePrática/release-patches/vX.Y.Z/`. Nenhuma chave privada nos patches. Pode haver quebras de linha Base64.
4. Criar `release-notes.md` e **criar `manifest.json` por último**, contendo exatamente os hashes e tags. A criação do manifesto em `master` desencadeia automaticamente `.github/workflows/saudepratica-publish-release.yml`, que valida base/patch/APK, verifica assinatura e cria Release com `SaudePratica.apk`.
5. Verificar na API de Releases que `tag_name=vX.Y.Z`, `draft=false`, `prerelease=false`, `assets[].name=SaudePratica.apk` e `releases/latest` aponta para a nova versão. Não confiar apenas em "GitHub Action concluído".
6. Link permanente: https://github.com/DanBar1971/AndroidAppDev/releases/latest/download/SaudePratica.apk

## Regras funcionais
- Cada perfil de utente e documentos médicos pertencem a IDs estáveis; não misturar dados de familiares na sincronização.
- Módulos e novidades devem preservar medicação, notificações, e os dados da v1.9.78; testar regressões.
- O módulo Lares/ERPI, SAD e Centro de Dia é uma das funcionalidades atuais: `SaúdePrática/MODULO_LARES_APOIO_TERCEIRA_IDADE.md` e `MediAgenda/v97_overrides/` (v1.9.79).
- Em qualquer comunicação de estado, distinguir claramente **código enviado / APK compilada / APK assinada / Release publicada**.

## Proteção obrigatória de imagens e estabilidade (v1.9.80)
- Não voltar a produzir APKs sem a arte original: `saude_background.png`, `saude_icon.png`, `saude_splash.png`.
- Recursos já guardados no repositório em `MediAgenda/v98_overrides/branding/` (Base64 PNG) e restaurados durante `apply_v98.py`; são cópias otimizadas visualmente das imagens originais fornecidas pelo utilizador. Consultar o Projeto/Library para obter os PNGs originais de resolução plena caso necessário.
- O workflow `.github/workflows/mediagenda-v1969.yml` bloqueia a publicação se `MediAgenda/tools/verify_graphics.py` falhar ou se a APK compilada não incluir pelo menos três imagens da marca reais.
- Última Release confirmada: **v1.9.80**, ficheiro exato `SaudePratica.apk`, SHA256 `1f523d51ef8168dcc6c864e049f7914f6d0e1abb0e3c7a62437c2a30803cba9f`.
- **Lares**: catálogo partilhado da instituição, residentes explicitamente associados, selecionar Camy ou outro familiar; cada fatura, nota, documento e valência pertence exclusivamente a um perfil ID.
- **Atalhos**: ordem personalizada persistente com Subir/Descer.
