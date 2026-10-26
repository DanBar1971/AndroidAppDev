# Publicar APK da SaúdePrática no GitHub Releases

Sim: o GitHub permite alojar ficheiros `.apk` através de **Releases**. O GitHub Actions também consegue automatizar a publicação depois de ter os segredos de assinatura configurados.

## Publicação manual segura

1. Compila e valida a nova versão Android.
2. Assina a APK com a **mesma chave de assinatura** usada nas versões anteriores; sem essa chave não é possível atualizar a app instalada, preservando os dados.
3. Abre https://github.com/DanBar1971/AndroidAppDev/releases e seleciona **Draft a new release**.
4. Cria uma tag com o formato `v1.9.75` (usar a versão real), escreve as novidades, e anexa **apenas a APK assinada**. Não publiques a keystore, passwords, backups, dados médicos nem documentos pessoais.
5. Para manter um endereço de download constante, chama ao ficheiro anexo **`SaudePratica.apk`**, qualquer que seja a versão.
6. Publica como release normal (não pre-release), se estiver validada e pronta para os utilizadores.

## Ligações para partilhar

Página da versão mais recente:

https://github.com/DanBar1971/AndroidAppDev/releases/latest

**Download direto da APK mais recente**, quando existir uma release publicada contendo `SaudePratica.apk`:

https://github.com/DanBar1971/AndroidAppDev/releases/latest/download/SaudePratica.apk

**Importante:** estas ligações só funcionam para descarregar uma APK depois de a release correspondente ter sido publicada. GitHub Actions → Artifacts não é equivalente a Releases (artefactos expiram e podem requerer autenticação).

Se o repositório for privado, o destinatário precisa de acesso autorizado ao GitHub.

## Futuras atualizações automáticas

- Configurar segredos privados no GitHub Actions para a keystore, a password e o alias de assinatura.
- Usar os gráficos originais no build de produção, validando-os antes de publicar.
- Publicar com GitHub Actions / GitHub CLI `gh release create` e asset `SaudePratica.apk`.
- Nunca publicar APKs não assinadas como ficheiros prontos a instalar.
- Manter o identificador Android e a assinatura para permitir instalar por cima sem perder dados.

**Atenção:** alterar o nome do projeto/aplicação não obriga a alterar o package ID do Android. A pasta técnica `MediAgenda` mantém-se para o build.
