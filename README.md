# myfabric-demo-java-client
Hyperledger Fabric联盟链定制化示例Java客户端

## 运行

1. 启动[myfabric-demo-chain](https://github.com/fftt2017/myfabric-demo-chain)

2. 修改 myfabric-demo-java-client/src/main/java/litong/hyperledger/fabric/example/Demo.java代码中 ```private static final String HOST = "192.168.99.101";```为运行docker ip地址。

如果[myfabric-demo-chain](https://github.com/fftt2017/myfabric-demo-chain)重新生成组织身份证书。需要复制相关证书文件到```certificate```目录下，可参考```certificate.bat```。
