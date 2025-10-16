#!/usr/bin/env bash
set -euo pipefail

# Instala la APK debug del módulo app/phone en un dispositivo conectado via adb.
# Usa la tarea de Gradle de instalación del variant si existe (instala splits automáticamente),
# si no, arma el APK y lo instala con adb.

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_DIR"

# Parse arguments
SDK_DIR=""
usage() {
  cat <<EOF
Usage: $0 [--sdk /path/to/android/sdk]

Options:
  --sdk PATH   Temporarily write local.properties with sdk.dir=PATH before running Gradle
  -h, --help   Show this help message
EOF
}
while [[ $# -gt 0 ]]; do
  case "$1" in
    --sdk)
      SDK_DIR="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1"
      usage
      exit 1
      ;;
  esac
done

if [ -n "$SDK_DIR" ]; then
  # backup existing local.properties if present
  if [ -f local.properties ]; then
    cp local.properties "local.properties.bak.$(date +%s)"
    echo "Backed up existing local.properties to local.properties.bak.$(date +%s)"
  fi
  echo "sdk.dir=$SDK_DIR" > local.properties
  echo "Wrote local.properties with sdk.dir=$SDK_DIR"
fi

# Helpers
run_gradle() {
  ./gradlew "$@"
}

echo "Verificando adb..."
if ! command -v adb >/dev/null 2>&1; then
  echo "adb no está en PATH. Instala platform-tools y vuelve a intentar."
  exit 1
fi

DEVICE_COUNT=$(adb devices | sed '1d' | awk 'NF{print $1}' | wc -l)
if [ "$DEVICE_COUNT" -eq 0 ]; then
  echo "No hay dispositivos adb conectados. Conecta tu móvil y activa Depuración USB (ADB)."
  adb devices
  exit 1
fi

echo "Dispositivo detectado:" 
adb devices | sed '1d' | awk 'NF{print $1, $2}'

# Prefer variant with flavor 'libre' if present (repo uses a flavour libre as default)
echo "Buscando tareas de instalación disponibles para :app:phone (install*Debug)..."
# List available tasks for the module and extract install*Debug tasks (exclude AndroidTest variants)
AVAILABLE_INSTALL_TASKS=$(./gradlew :app:phone:tasks --all --console=plain --no-daemon 2>/dev/null | \
  sed -n '1,4000p' | grep -E '^[[:space:]]*install.*Debug' | awk '{print $1}' | grep -v AndroidTest | uniq || true)

if [ -n "$AVAILABLE_INSTALL_TASKS" ]; then
  echo "Tareas de instalación detectadas:"
  echo "$AVAILABLE_INSTALL_TASKS"
  for t in $AVAILABLE_INSTALL_TASKS; do
    FULL_TASK=":app:phone:$t"
    echo "Intentando tarea Gradle: $FULL_TASK"
    if run_gradle --no-daemon -q "$FULL_TASK"; then
      echo "Instalación finalizada mediante tarea Gradle: $FULL_TASK"
      exit 0
    else
      echo "Tarea $FULL_TASK falló o no está disponible. Intentando siguiente opción..."
    fi
  done
else
  echo "No se detectaron tareas install*Debug en :app:phone (o no se pudo listar tareas)."
fi

# Si llegamos aquí, no pudimos usar la tarea install. Hacemos assemble y buscamos APK(s).
ASSEMBLE_TASKS=(
  ":app:phone:assembleLibreDebug"
  ":app:phone:assembleDebug"
)

ASSETS_DIR="app/phone/build/outputs/apk"
APK_PATHS=()

for t in "${ASSEMBLE_TASKS[@]}"; do
  echo "Ejecutando: $t"
  if run_gradle --no-daemon -q "$t"; then
    # buscar apks generados
    echo "Buscando APKs en $ASSETS_DIR"
    while IFS= read -r -d '' file; do
      APK_PATHS+=("$file")
    done < <(find "$ASSETS_DIR" -name '*.apk' -print0 || true)
    if [ ${#APK_PATHS[@]} -gt 0 ]; then
      break
    fi
  else
    echo "Assemble $t falló o no está disponible."
  fi
done

if [ ${#APK_PATHS[@]} -eq 0 ]; then
  echo "No se encontraron APKs tras el assemble. Revisa la construcción en detalle."
  exit 1
fi

# Si hay múltiples APKs (splits), usamos install-multiple. Si solo hay una, usamos install -r
if [ ${#APK_PATHS[@]} -gt 1 ]; then
  echo "Se encontraron múltiples APKs (splits), instalando con adb install-multiple -r"
  printf '%s\n' "${APK_PATHS[@]}"
  adb install-multiple -r "${APK_PATHS[@]}"
else
  echo "Instalando APK único: ${APK_PATHS[0]}"
  adb install -r "${APK_PATHS[0]}"
fi

echo "Instalación completada."
exit 0
