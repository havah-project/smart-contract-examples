# merkle airdrop

This subproject provide a sample implementation of merkle airdrop. Contract is compatible with [merkletreejs](https://github.com/merkletreejs/merkletreejs?tab=readme-ov-file).

To make merkle tree, visit to webpage of [MerkleTree.js example](https://lab.miguelmota.com/merkletreejs/example/). and choose Keccak-256 hash function, then select hashLeaves, sortLeaves, sortPairs options.

## Sample

Input
```
[
    "hx3e65ce9ff07186df3ee2bda02d20420e2da5da8010000000000000000000",
    "hx34e7759532571fe15c129a045627b437869c818c20000000000000000000",
    "hx1dc6d2f7fe9e1f969279e816b3fdbfbe4134bf3d30000000000000000000",
    "hxe0afc6ff8a605f24abd42b2cf2f1e0de11a797ff40000000000000000000",
    "hx36b8ecb38486d273c4cb87fd8d2509b2e441c02d50000000000000000000"
]
```

MerkleTree
```
Tree
└─ 8935f1f69424db7af043d2194308fd86cc3ac83b7992a059353605f303c76bab
   ├─ f199d54237e659b413c31fcf754fe8ded9a038d459d1e4f25eee5453c8720489
   │  ├─ 2042a4a20d55fb2674893a3546128f9b35a1be4268ad4b4bade94197a819d8c3
   │  │  ├─ 1a80ebcb34050d07dc991be3ae963b14365ffbae2429539c1edbf38332275644
   │  │  └─ 45c58c145c622424d4fd4cdcb7a68f39460b20cba50317e79e5ff9c13b0d510b
   │  └─ da2e76cab6fbf4e4b0f004c7233ef5ead63a0c044e473fe13b8900e0b5e54283
   │     ├─ 473757d59afeaeb75454bb32452b0ac207b1b91b6e8e1e18e932748fc0b3d64c
   │     └─ 5a6abcfd8c8c89d6e936619996cd737f65eb8cd13a554876e316f566372fb9b7
   └─ d58456e77fe12f0297cc516f90187acf1f87459b2871ba931e0f312af759652c
      └─ d58456e77fe12f0297cc516f90187acf1f87459b2871ba931e0f312af759652c
         └─ d58456e77fe12f0297cc516f90187acf1f87459b2871ba931e0f312af759652c
```
