cd C:\github\scratch\hyperledger\fabric-sdk-java-demo

copy /y C:\vb_ubuntu\myshare\myfabric-demo\org0\data\org0-ca-chain.pem certificate\org0-ca-chain.pem
copy /y C:\vb_ubuntu\myshare\myfabric-demo\org1\data\org1-ca-chain.pem certificate\org1-ca-chain.pem

copy /y C:\vb_ubuntu\myshare\myfabric-demo\org1\data\admin\msp\keystore\* certificate\admin_sk
copy /y C:\vb_ubuntu\myshare\myfabric-demo\org1\data\admin\msp\signcerts\* certificate\admin_cert.pem
copy /y C:\vb_ubuntu\myshare\myfabric-demo\org1\data\admin\tls\client.crt certificate\admin_client.crt
copy /y C:\vb_ubuntu\myshare\myfabric-demo\org1\data\admin\tls\client.key certificate\admin_client.key

copy /y C:\vb_ubuntu\myshare\myfabric-demo\org1\data\user\msp\keystore\* certificate\user_sk
copy /y C:\vb_ubuntu\myshare\myfabric-demo\org1\data\user\msp\signcerts\* certificate\user_cert.pem
copy /y C:\vb_ubuntu\myshare\myfabric-demo\org1\data\user\tls\client.crt certificate\user_client.crt
copy /y C:\vb_ubuntu\myshare\myfabric-demo\org1\data\user\tls\client.key certificate\user_client.key