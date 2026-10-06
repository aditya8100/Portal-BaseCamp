#!/bin/sh
# Find the Portal on the LAN by probing for ADB-over-TCP (port 5555).
# Usage: ./find-portal.sh [subnet]   (default: 192.168.1)
# Prints "HIT <ip>:<port>" for each host with ADB-over-TCP open (probes
# 5555 and 5566 — a past session typo'd the port once), then connect with:
#   adb connect <ip>:<port>
# Fast path: probe current ARP entries only (~2s). Falls back to a ping
# sweep to repopulate ARP if nothing answers.
SUBNET="${1:-192.168.1}"
probe() {
  arp -an | grep -oE "$SUBNET\.[0-9]+" | sort -u | xargs -P32 -I{} sh -c "for p in 5555 5556; do nc -z -w1 {} \$p 2>/dev/null && echo HIT {}:\$p; done"
}
HIT=$(probe)
if [ -z "$HIT" ]; then
  seq 2 254 | xargs -P64 -I{} ping -c1 -W800 "$SUBNET.{}" >/dev/null 2>&1
  HIT=$(probe)
fi
echo "$HIT"
exit 0
