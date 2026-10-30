# REPOSITÓRIO ANTIGO — SaúdePrática migrou (30/10/2026)

**Todas as novas alterações, builds e releases devem ocorrer em https://github.com/DanBar1971/SaudePratica.** A versão de transição v1.9.119 foi publicada aqui para atualizar as instalações existentes para o novo canal. Este repositório fica como arquivo; não publicar novas releases Android aqui por engano. O workflow automático de lançamento foi colocado em execução manual apenas. O histórico antigo é mantido para segurança e auditoria.

---

# Project continuity for Android Apps / SaúdePrática

When working on **SaúdePrática** in this repository, read these files first:
1. [SaúdePrática/INSTRUCOES_DE_CONTINUIDADE.md](SaúdePrática/INSTRUCOES_DE_CONTINUIDADE.md)
2. [SaúdePrática/PUBLICAR_APK_GITHUB.md](SaúdePrática/PUBLICAR_APK_GITHUB.md)
3. [SaúdePrática/README.md](SaúdePrática/README.md)

The project owner explicitly requires:
- Continue across ChatGPT conversations in the same project, **searching the existing repository and Project/Library for code, APKs, graphics and the original Android keystore before asking for files again**.
- **Always publish signed updates to GitHub Releases**, never treat a build artifact, a ZIP or a downloaded APK as the completed release.
- After publishing, confirm latest tag, release status, public `SaudePratica.apk` asset, SHA-256, and permanent download link.
- Keep the original Android certificate and package ID for in-place upgrades; never commit private keystores, passwords, private data or patient files.

The signed-delta publication method was used successfully for **v1.9.77, v1.9.78, v1.9.79 and v1.9.80**, and is documented in the files linked above. Use it when the Actions runner lacks the original signing secret. Do not assert publication until verified.

## Mandatory branded artwork gate
- Restore `saude_background.png`, `saude_icon.png` and `saude_splash.png` using `MediAgenda/v98_overrides/branding/*.png.b64` and the v98 build patch. Assets were provided by the project owner; never request them again without first checking repository and Project/Library.
- Before every Release, run `MediAgenda/tools/verify_graphics.py`, verify the three branded PNGs are inside the **compiled APK**, and check original signing certificate. Do not publish a build with placeholder graphics.
- Latest verified published release: v1.9.80 (26 Oct 2026); fixed asset `SaudePratica.apk`.

## Regra permanente — imagens fiéis à app (28/10/2026)
- **NUNCA criar mockups com ecrãs fictícios da SaúdePrática**: as imagens que mostram um telemóvel têm de reproduzir a interface Android real, com capturas originais aprovadas. Se não existirem capturas fiáveis, usar cartazes sem ecrãs/telemóveis ilustrados.
- No módulo `Opções → Dicas e Informações` os cartazes 9:16 remotos ocupam quase toda a janela, sem título, e têm apenas botões Android no rodapé. O histórico fica guardado localmente no telemóvel e disponível para consulta posterior.
- Conteúdo remoto público em https://github.com/DanBar1971/SaudePratica/tree/main/info_saude/; nunca enviar dados clínicos ou dados de perfis para esse repositório.
