#!/usr/bin/env bash
# Publish Minecraft Console Client sebagai aplikasi native self-contained untuk Android (bionic) arm64.
set -euo pipefail

export DOTNET_NOLOGO=1
export DOTNET_CLI_TELEMETRY_OPTOUT=1

SRC="${MCC_SRC:-mcc-src}"
OUT="$PWD/build/mcc-out"
rm -rf "$OUT"

echo "== dotnet $(dotnet --version)"

CSPROJ="$SRC/MinecraftClient/MinecraftClient.csproj"
# Tidak ada runtime pack ASP.NET Core untuk linux-bionic; lepas referensinya bila ada.
if grep -q "Microsoft.AspNetCore.App" "$CSPROJ"; then
  echo "== Melepas FrameworkReference Microsoft.AspNetCore.App dari csproj"
  sed -i '/Microsoft\.AspNetCore\.App/d' "$CSPROJ"
  echo "== Pemakaian namespace AspNetCore di kode (info):"
  grep -rl "Microsoft.AspNetCore" "$SRC/MinecraftClient" --include=*.cs | head -20 || true
fi
dotnet publish "$SRC/MinecraftClient/MinecraftClient.csproj" \
  -c Release \
  -r linux-bionic-arm64 \
  --self-contained true \
  -p:PublishSingleFile=false \
  -p:IncludeNativeLibrariesForSelfExtract=false \
  -p:UseAppHost=true \
  -p:DebugType=None \
  -p:DebugSymbols=false \
  -p:InvariantGlobalization=true \
  -p:TieredPGO=false \
  -p:SatelliteResourceLanguages=en \
  -o "$OUT"

echo "== Hasil publish:"
ls "$OUT" | head -80
test -f "$OUT/MinecraftClient" || { echo "::error::apphost MinecraftClient tidak terbentuk"; exit 1; }
