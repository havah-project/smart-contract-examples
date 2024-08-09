# Smart Contract Examples

This repository contains Smart Contract examples written in Java. 
Most example codes use sclib, a library of reusable and secure contract components. [link](https://github.com/havah-project/sclib-token)

## Requirements

You need to install JDK 11 or later version. Visit [OpenJDK.net](http://openjdk.java.net/) for prebuilt binaries.
Or you can install a proper OpenJDK package from your OS vendors.

In macOS:
```
$ brew tap AdoptOpenJDK/openjdk
$ brew cask install adoptopenjdk11
```

In Linux (Ubuntu 20.04):
```
$ sudo apt install openjdk-11-jdk
```

## How to Run

### 1. Build the project

```
$ ./gradlew build
```
The compiled jar bundle will be generated at `./hello-world/build/libs/hello-world-0.1.0.jar`.

### 2. Optimize the jar

You need to optimize your jar bundle before you deploy it to local or HAVAH networks.
This involves some pre-processing to ensure the actual deployment successful.

`gradle-javaee-plugin` is a Gradle plugin to automate the process of generating the optimized jar bundle.
Run the `optimizedJar` task to generate the optimized jar bundle.

```
$ ./gradlew optimizedJar
```
The output jar will be located at `./hello-world/build/libs/hello-world-0.1.0-optimized.jar`.

### 3. Deploy the optimized jar

#### Using `goloop` CLI command

Now you can deploy the optimized jar to HAVAH networks that support the Java Smart Contract execution environment.
Assuming you are running a local network that is listening on port 9082 for incoming requests,
you can create a deploy transaction with the optimized jar and deploy it to the local network as follows.

```
$ goloop rpc sendtx deploy ./hello-world/build/libs/hello-world-0.1.0-optimized.jar \
    --uri http://localhost:9082/api/v3 \
    --key_store <your_wallet_json> --key_password <password> \
    --nid 3 --step_limit=1000000 \
    --content_type application/java \
    --param name=Alice
```

#### Using `deployJar` extension

Starting with version `0.7.2` of `gradle-javaee-plugin`, you can also use the `deployJar` extension to specify all the information required for deployment.

```groovy
deployJar {
    endpoints {
        local {
            uri = 'http://localhost:9082/api/v3'
            nid = 3
        }
    }
    keystore = rootProject.hasProperty('keystoreName') ? "$keystoreName" : ''
    password = rootProject.hasProperty('keystorePass') ? "$keystorePass" : ''
    parameters {
        arg('name', 'Alice')
    }
}
```

Now you can run `deployToLocal` task as follows.

```
$ ./gradlew hello-world:deployToLocal -PkeystoreName=<your_wallet_json> -PkeystorePass=<password>

> Task :hello-world:deployToLocal
>>> deploy to http://localhost:9082/api/v3
>>> optimizedJar = ./hello-world/build/libs/hello-world-0.1.0-optimized.jar
>>> keystore = <your_wallet_json>
Succeeded to deploy: 0x699534c9f5277539e1b572420819141c7cf3e52a6904a34b2a2cdb05b95ab0a3
SCORE address: cxd6d044b01db068cded47bde12ed4f15a6da9f1d8
```

**[Note]** If you want to deploy to Vega testnet, use the following configuration for the endpoint and run `deployToVega` task.
```groovy
deployJar {
    endpoints {
        vega {
            uri = 'https://ctz.vega.havah.io/api/v3/'
            nid = 0x101
        }
        ...
    }
}
```

### 4. Verify the execution

Check the deployed Smart Contract address first using the `txresult` command.
```
$ goloop rpc txresult <tx_hash> --uri http://localhost:9082/api/v3
{
  ...
  "scoreAddress": "cxaa736426a9caed44c59520e94da2d64888d9241b",
  ...
}
```

Then you can invoke `getGreeting` method via the following `call` command.
```
$ goloop rpc call --to <score_address> --method getGreeting --uri http://localhost:9082/api/v3
"Hello Alice!"
```

## Testing

Two testing frameworks are provided as to be used for different purposes:
one is for unit testing and the other is for integration testing.

### Unit testing

Now [`javaee-unittest`](https://github.com/icon-project/javaee-unittest) artifact is used to perform the unit testing.

Here are the sample unit test cases.
  - [HelloWorld](hello-world/src/test/java/com/iconloop/score/example/AppTest.java)
  - [MultisigWallet](multisig-wallet/src/test/java/com/iconloop/score/example/MultiSigWalletTest.java)
  - [Crowdsale](sample-crowdsale/src/test/java/com/iconloop/score/example/SampleCrowdsaleTest.java)
  - [HSP20BurnableToken](hsp20-token/src/test/java/io/havah/contract/example/HSP20BurnableTest.java)
  - [HSP721Token (NFT)](hsp721-token/src/test/java/io/havah/contract/example/HSP721BasicTest.java)

### Integration testing

[`testinteg`](testinteg) subproject can be used for the integration testing.
It assumes there is a running HAVAH network (either local or remote) that can be connected for the testing.
It uses the [ICON SDK for Java](https://github.com/icon-project/icon-sdk-java) to interact with the network.
The [default configuration](testinteg/conf/env.props) is for [gochain-local](https://github.com/icon-project/gochain-local) network.
If you want to change this configuration, either modify the configuration file directly
or set the proper system property (`env.props`) when you run the integration testing
(see [example](https://github.com/havah-project/smart-contract-examples/blob/14c4df50b146c12c27a040410411271e87efa94a/multisig-wallet/build.gradle#L69)).

Here are the sample integration test cases.
  - [MultisigWallet](multisig-wallet/src/intTest/java/foundation/icon/test/cases/MultiSigWalletTest.java)
  - [Crowdsale](sample-crowdsale/src/intTest/java/foundation/icon/test/cases/CrowdsaleTest.java)
  - [HSP721Token (NFT)](hsp721-token/src/intTest/java/io/havah/test/cases/HSP721TokenTest.java)

Use `integrationTest` task to run the integration testing.
Here is the example of invoking the MultisigWallet integration testing.
```
$ ./gradlew multisig-wallet:integrationTest
```

## Java Smart Contract Structure


| Name                      | Java Smart Contract         |
|---------------------------|-----------------------------|
| External decorator        | `@External`                 |
| - (readonly)              | `@External(readonly=true)`  |
| Payable decorator         | `@Payable`                  |
| Eventlog decorator        | `@EventLog`                 |
| - (indexed)               | `@EventLog(indexed=1)`      |
| fallback signature        | `void fallback()`           |
| Smart Contract initialize | define a public constructor |
| Default parameters        | `@Optional`                 |

**[NOTE]** All external Java methods must have a `public` modifier, and should be instance methods.

### How to invoke a external method of another Smart Contract

One Smart Contract can invoke an external method of another Smart Contract using the following APIs.

```java
// [package score.Context]
public static Object call(Address targetAddress, String method, Object... params);

public static Object call(BigInteger value,
                          Address targetAddress, String method, Object... params);
```


## projects description

### hello-world

First sample for JAVA contract

### hsp20-token

This subproject contains the Java implementation of HSP-20 Token

### hsp721-token

This subproject contains the Java implementation of HSP-721 Token

### hsp1155-token

This subproject contains the Java implementation of HSP-1155 Token

### multisig-wallet

This subproject contains the Java implementation of Multisignature Wallet

### simple dex

This subprojects contains the Java implementation of simple DEX(Decentralized Exchanges). We adopted the swap mechanism inspired by [Uniswap v1](https://hackmd.io/@HaydenAdams/HJ9jLsfTz).

- dex/liquidity-pool
    - HSP20 Token-HSP20 Token or HSP20 Token-Native HVH pair pool contract.
- dex/liquidity-token
    - Liquidity Token for pool. this token represent a liquidity providers contribution to an liquidity pool.
- dex/pool-factory
    - Factory for liquidity pool.

### merkle airdrop

This subprojects contains the Java implementation of merkle airdrop. Contract is compatible with [merkletreejs](https://github.com/merkletreejs/merkletreejs?tab=readme-ov-file).

### vesting

This subproject contains the Java implementation of token vesting.

## References

* [Java Smart Contract Overview](https://docs.google.com/presentation/d/1S24vCTcPJ5GOGfPu1sApJLwyOTTdgYEf/export/pdf)
* [Smart Contract API document](https://www.javadoc.io/doc/foundation.icon/javaee-api)
* [Gradle plugin for JavaEE](https://github.com/icon-project/gradle-javaee-plugin)
* [A Java Smart Contract Library for Standard Tokens](https://github.com/havah-project/sclib-token)
* [scorex package for Java Smart Contract](https://github.com/icon-project/javaee-scorex)
* [An Unit Testing Framework for Java Smart Contract](https://github.com/icon-project/javaee-unittest)
* [A fast and small JSON parser and writer for Java](https://github.com/sink772/minimal-json)
* [`goloop` CLI command reference](https://github.com/havah-project/goloop-havah/blob/main/doc/goloop_cli.md)

## License

This project is available under the [Apache License, Version 2.0](LICENSE).
