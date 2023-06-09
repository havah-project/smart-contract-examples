# Simple DEX(Decentralized Exchanges) Example

This repository contains Simple DEX example written in Java. We adopted the swap mechanism inspired by [Uniswap v1](https://hackmd.io/@HaydenAdams/HJ9jLsfTz).

## subprojects

- dex/liquidity-pool
    - HSP20 Token-HSP20 Token or HSP20 Token-Native HVH pair pool contract.
- dex/liquidity-token
    - Liquidity Token for pool. this token represent a liquidity providers contribution to an liquidity pool.
- dex/pool-factory
    - Factory for liquidity pool.

## Liquidity Pool

You need to deploy pool and call initialize() function to initialize pool contract. also, pool conatract must be registered as minter of lpToken.

``` java
public void initialize(Address _baseToken, Address _quoteToken, Address _lpToken)
```

add liquidity:

``` java
public void add(BigInteger _baseValue, BigInteger _quoteValue)
```

remove liquidity:

``` java
public void remove(BigInteger _lpAmount)
```

swap:

``` java
public void swap(Address _receiver, Address _fromToken, Address _toToken, BigInteger _value, BigInteger _minimumReceive)
```

If you want swap native coin(HVH), input "cx0000000000000000000000000000000000000000" to token address parameter(_fromToken or _toToken).