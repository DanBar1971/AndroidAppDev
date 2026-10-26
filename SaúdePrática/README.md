# SaúdePrática — aplicação Android

Projeto oficial: **SaúdePrática** (anteriormente conhecido internamente como *MediAgenda*).

## Código-fonte e compilação

- [Histórico e módulos de compilação](../MediAgenda/)
- [Compilação Android da versão atual](../.github/workflows/mediagenda-v1969.yml)
- [Módulo Atalhos Importantes, v1.9.78](../MediAgenda/v96_overrides/apply_v96.py)

A pasta `MediAgenda` é **legada** e permanece temporariamente para que as versões antigas e o processo de reconstrução do projeto continuem a funcionar. O nome público e de produto é **SaúdePrática**. O pacote instalado no Android mantém-se igual, para preservar atualizações e dados.

**Próxima migração:** transferir os ficheiros do histórico de compilação para este diretório sem interromper as compilações nem invalidar scripts históricos.

## Downloads no GitHub

- [Releases / APKs publicadas](https://github.com/DanBar1971/AndroidAppDev/releases)
- [Como publicar o APK assinado e manter um URL fixo](./PUBLICAR_APK_GITHUB.md)

O download direto só estará disponível quando uma versão assinada for anexada a uma Release. Nunca publicar chaves de assinatura ou dados dos perfis.

## Publicação automática verificada

Desde a versão **v1.9.77**, as Releases podem ser publicadas automaticamente pelo [workflow de publicação](../.github/workflows/saudepratica-publish-release.yml) depois de preparada uma APK assinada e verificados os respetivos hashes SHA-256. Mantém-se o certificado privado fora do repositório; o workflow recusa APKs alteradas, não assinadas ou com hashes inesperados.

**Regra do projeto:** uma nova versão não está concluída sem verificar a publicação no GitHub Releases, o asset `SaudePratica.apk` e a ligação permanente [Descarregar última versão](https://github.com/DanBar1971/AndroidAppDev/releases/latest/download/SaudePratica.apk).

[Procedimento de publicação para as versões futuras](./PUBLICAR_APK_GITHUB.md)

## Atalhos Importantes (v1.9.78)

- Ecrã inicial: **🔗 Atalhos Importantes**, para abrir URLs no navegador predefinido do Android.
- Configuração: **Opções → Atalhos Importantes**. Permite adicionar, editar e eliminar links.
- Quatro atalhos inicialmente disponíveis: ADSE Direta, CGA Direta, Portal das Finanças/IRS e APATI Castanheira.
- Favicons carregados em segundo plano e guardados na cache; fallback para serviço de favicons da Google ou para o ícone 🌐.
- Configuração guardada localmente, independente dos perfis de saúde; não é incluída na sincronização familiar.

**Release atual:** [v1.9.78](https://github.com/DanBar1971/AndroidAppDev/releases/tag/v1.9.78). O arquivo publicado mantém o nome fixo `SaudePratica.apk`.
