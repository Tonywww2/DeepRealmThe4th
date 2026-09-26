param(
    [Parameter(Mandatory=$true)][ValidateSet('forge', 'neoforge')][string]$Loader,
    [Parameter(Mandatory=$true)][string[]]$Command,
    [ValidateSet('default','compat')][string]$Profile='default'
)

# Development helper: deliberately restricted to this project's loopback-only servers.
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$runName = if ($Profile -eq 'compat') { "$Loader-compat-server" } else { "$Loader-server" }
$properties = Get-Content -Encoding UTF8 -LiteralPath (Join-Path $projectRoot "run/$runName/server.properties")
$settings = @{}
foreach ($line in $properties) {
    if ($line -match '^([^#!][^=]*)=(.*)$') { $settings[$matches[1]] = $matches[2] }
}
if ($settings['server-ip'] -ne '127.0.0.1' -or $settings['enable-rcon'] -ne 'true') {
    throw 'This helper only supports an explicitly loopback-bound development server with RCON enabled.'
}

$client = [System.Net.Sockets.TcpClient]::new()
try {
    $client.Connect('127.0.0.1', [int]$settings['rcon.port'])
    $stream = $client.GetStream()
    $stream.ReadTimeout = 120000
    $stream.WriteTimeout = 10000
    function Read-Exact([int]$count) {
        $buffer = [byte[]]::new($count)
        $offset = 0
        while ($offset -lt $count) {
            $received = $stream.Read($buffer, $offset, $count - $offset)
            if ($received -eq 0) { throw 'RCON connection closed.' }
            $offset += $received
        }
        return ,$buffer
    }
    function Read-Packet {
        $length = [BitConverter]::ToInt32((Read-Exact 4), 0)
        if ($length -lt 10 -or $length -gt 1048576) { throw "Invalid RCON packet length: $length" }
        $data = Read-Exact $length
        return @{ Id = [BitConverter]::ToInt32($data, 0); Type = [BitConverter]::ToInt32($data, 4);
            Text = [Text.Encoding]::UTF8.GetString($data, 8, $length - 10) }
    }
    function Send-Packet([int]$id, [int]$type, [string]$message) {
        $body = [Text.Encoding]::UTF8.GetBytes($message)
        $packet = [byte[]]([BitConverter]::GetBytes([int]($body.Length + 10)) +
            [BitConverter]::GetBytes($id) + [BitConverter]::GetBytes($type) + $body + [byte[]](0, 0))
        $stream.Write($packet, 0, $packet.Length)
    }
    Send-Packet 1 3 $settings['rcon.password']
    do { $auth = Read-Packet } while ($auth.Type -ne 2)
    if ($auth.Id -eq -1) { throw 'RCON authentication failed.' }
    $id = 2
    foreach ($entry in $Command) {
        Write-Output "> $entry"
        Send-Packet $id 2 $entry
        if ($entry.Trim() -eq 'stop') {
            $reply = Read-Packet
            if ($reply.Id -ne $id) { throw 'Unexpected RCON response ID.' }
            Write-Output $reply.Text
        } else {
            # A read-only sentinel delimits multi-packet responses (ecology reports exceed 4096 chars).
            $body = [Text.StringBuilder]::new()
            $reply = Read-Packet
            if ($reply.Id -ne $id) { throw 'Unexpected RCON response ID.' }
            [void]$body.Append($reply.Text)
            # Wait for the first reply before sending another request: MC rejects coalesced requests.
            Send-Packet ($id + 1) 2 'list'
            do {
                $reply = Read-Packet
                if ($reply.Id -eq $id) { [void]$body.Append($reply.Text) }
                elseif ($reply.Id -ne $id + 1) { throw 'Unexpected RCON response ID.' }
            } while ($reply.Id -ne $id + 1)
            Write-Output $body.ToString()
        }
        $id += 2
    }
} finally {
    $client.Dispose()
}
