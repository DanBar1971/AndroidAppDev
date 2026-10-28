# Website de apresentação da SaúdePrática

Site público, estático, em português de Portugal, preparado para GitHub Pages. Os ecrãs ilustrativos utilizam dados fictícios, não capturas de informações pessoais. Inclui ligação para a APK mais recente (`releases/latest/download/SaudePratica.apk`), com atualização visual opcional pela API pública do GitHub.

## Identidade visual

As três imagens originais estão no repositório como base64 em `MediAgenda/v98_overrides/branding/{saude_icon,saude_splash,saude_background}.png.b64`. O workflow descodifica-as para `assets/` durante a publicação. Os ficheiros de assinatura privada, palavras-passe e dados de saúde **nunca** são copiados para o site.

## Publicar

No repositório GitHub, abrir `Settings → Pages → Build and deployment → Source` e selecionar **GitHub Actions** (configuração inicial, uma vez apenas). A partir daí, `.github/workflows/saudepratica-pages.yml` publica automaticamente as alterações em `website/**` e é possível voltar a executar o workflow manualmente.

Endereço esperado: https://danbar1971.github.io/AndroidAppDev/

Para prever localmente, criar um diretório `assets` ao lado de `index.html`, descodificar os três ficheiros base64 e abrir `index.html` num navegador. Os botões de download funcionam também sem JavaScript.