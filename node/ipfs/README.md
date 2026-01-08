# IPFS Node

This node utilizes [IPFS](https://ipfs.tech/) decentralized network technology.

The idea is that user of Mimir IPFS node runs an IPFS node as:
* either on the same workstation, for example the [IPFS Desktop](https://docs.ipfs.tech/install/ipfs-desktop/) or
  maybe installed by [some other means](https://docs.ipfs.tech/install/) (Homebrew, Docker, etc).
* or, has a dedicated node somewhere on his LAN

Either way, direct access to IPFS [Kubo RPC API](https://docs.ipfs.tech/reference/kubo/rpc/) is required.

There are two ways to use IPFS Node:
* as system node (where IPFS is used as main cache)
* as remote node (where IPFS is used as remote cache)
* mixed (for example, combined with file node)
