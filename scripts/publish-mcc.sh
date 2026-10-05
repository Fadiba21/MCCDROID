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
echo "== Isi awal csproj (bagian Project/Framework):"
grep -nE "<Project|FrameworkReference|AspNetCore|TargetFramework" "$CSPROJ" || true

# Paket ModelContextProtocol.AspNetCore butuh runtime ASP.NET Core yang tidak ada untuk linux-bionic.
# Lepas paketnya dan ganti host MCP bawaan dengan stub (bot MCP Server nonaktif di Android).
sed -i 's#Sdk="Microsoft.NET.Sdk.Web"#Sdk="Microsoft.NET.Sdk"#' "$CSPROJ"
sed -i '/Microsoft\.AspNetCore\.App/d; /ModelContextProtocol\.AspNetCore/d' "$CSPROJ"
HOST="$SRC/MinecraftClient/Mcp/MccEmbeddedMcpHost.cs"
if [ -f "$HOST" ]; then
  echo "== Mengganti MccEmbeddedMcpHost.cs dengan stub"
  cat > "$HOST" <<'CS'
using System;

namespace MinecraftClient.Mcp;

// Stub untuk Android: server MCP (ASP.NET Core) tidak didukung di linux-bionic.
public sealed class MccEmbeddedMcpHost
{
    private readonly MccMcpConfig config;

    public MccEmbeddedMcpHost(MccMcpConfig config, IMccMcpCapabilities capabilities)
    {
        this.config = config;
    }

    public bool IsRunning => false;

    public string Endpoint => $"http://{config.Transport.BindHost}:{config.Transport.Port}{config.Transport.Route}";

    public bool Start(out string? error)
    {
        error = "tidak didukung di Android";
        return false;
    }

    public bool Stop(out string? error)
    {
        error = null;
        return true;
    }
}
CS
fi
echo "== Setelah patch:"
grep -nE "<Project|FrameworkReference|AspNetCore" "$CSPROJ" || true

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
ls "$OUT" | wc -l
test -f "$OUT/MinecraftClient" || { echo "::error::apphost MinecraftClient tidak terbentuk"; exit 1; }
