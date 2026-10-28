#!/usr/bin/env python3
"""Source regression gates for SaúdePrática 1.9.105."""
from pathlib import Path
base=Path("build-src/MediAgenda_Android_v1/app")
code=base/"src/main/java/com/mediagenda/app"
clinical=(code/"WorkMedicineActivity.kt").read_text(encoding="utf-8")
assert "val clinicalEntries=obs.lines()" in clinical
assert "editClinicalRecord(null)" in clinical
assert 'putString("observations_by_unit",encoded)' in clinical
assert "updated.removeAt(index)" in clinical
assert 'putString("observation_number"' in clinical
assert 'ProfileStore(this).preferencesFor(patientId)' in clinical
for f in ("CareHomeActivity.kt","MonthlyCalendarActivity.kt"):
    s=(code/f).read_text(encoding="utf-8")
    assert "R.drawable.saude_background" in s,f
    assert "LayerDrawable" in s,f
paypal=[s for p in code.rglob("*.kt")
    if ("PayPal" in (s:=p.read_text(encoding="utf-8")))]
assert paypal and any("um café" in s for s in paypal)
assert all("cerveja" not in s.lower() and "🍺" not in s for s in paypal)
guide=(code/"SupportHelpActivity.kt").read_text(encoding="utf-8")
assert "vários registos" in guide
gradle=(base/"build.gradle.kts").read_text(encoding="utf-8")
assert 'versionCode = 115' in gradle
assert 'versionName = "1.9.105"' in gradle
assert 'applicationId = "com.mediagenda.app.stable"' in gradle
print("SaúdePrática v1.9.105 source regression gates OK")
