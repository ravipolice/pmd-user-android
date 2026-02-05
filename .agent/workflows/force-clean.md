---
description: Resolve persistent Gradle clean errors by stopping daemons and forcing build deletion
---

If you see an error like "Unable to delete directory ...\app\build", follow these steps:

1. Stop all Gradle daemons to release file locks:

```powershell
./gradlew --stop
```

1. Manually remove the build directory using PowerShell (Force):

```powershell
Remove-Item -Recurse -Force app\build
```

1. Run the clean task again:

```powershell
./gradlew clean
```
