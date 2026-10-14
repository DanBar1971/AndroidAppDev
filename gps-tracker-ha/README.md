# GPS Tracker HA - Android (Webhook edition v1.2.0)

App Android sem marcas pessoais para enviar localizações por POST HTTPS/JSON ao Home Assistant via webhook configurado no próprio telemóvel. NÃO publica URL privado, password, token ou ID de webhook neste repositório.

## Funcionalidade
- Latitude, longitude, precisão GPS, velocidade (quando disponível), bateria e data/hora.
- Intervalo 1 a 1440 minutos.
- Android Foreground Service com notificação e opção Parar.
- Configuração do URL guardada cifrada em AES-GCM usando Android Keystore.
- Sem arranque automático nem fila offline.
- Não precisa de utilizador/password: a identificação do webhook é o segredo.
- Só envia enquanto for ativado pelo utilizador. Sem anúncios ou analytics.

## Home Assistant
Criar uma automatização com um gatilho `webhook`, método POST, `local_only: false`.
O URL para a app deve ser o URL completo de `Definições > Home Assistant Cloud > Webhooks > Gerir`,
ou `https://TEU_DOMINIO_NABU_CASA/api/webhook/TEU_ID_SECRETO`.
Nunca colocar o URL real num repositório público.

Exemplo YAML para a automatização (substituir ID):

```yaml
alias: GPS Tracker HA
triggers:
  - trigger: webhook
    webhook_id: SUBSTITUIR_POR_ID_SECRETO
    allowed_methods: [POST]
    local_only: false
conditions:
  - condition: template
    value_template: >-
      {{ trigger.json is defined and
         -90 <= (trigger.json.latitude | float(999)) <= 90 and
         -180 <= (trigger.json.longitude | float(999)) <= 180 }}
actions:
  - action: device_tracker.see
    data:
      dev_id: android_gps
      gps:
        - "{{ trigger.json.latitude | float }}"
        - "{{ trigger.json.longitude | float }}"
      gps_accuracy: "{{ trigger.json.gps_accuracy | default(50) | float }}"
mode: queued
```

## Compilação
Workflow `.github/workflows/gps-tracker-ha-build.yml` executado por push na branch `gps-tracker-ha-apk`.
O APK debug (assinado com chave de debug) aparece em `downloadable/GPS-Tracker-HA-v1.2.0.apk` quando a execução e publicação terminarem.
Isto é um build de teste, não um pacote de loja Play Store. Requer localização e ligação à Internet.
