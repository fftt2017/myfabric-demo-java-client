set chain_dir=<myfabric-demo-chain dir path>
set java_client_dir=<myfabric-demo-java-client dir path>

copy /y %chain_dir%\org0\data\org0-ca-chain.pem %java_client_dir%\certificate\org0-ca-chain.pem
copy /y %chain_dir%\org1\data\org1-ca-chain.pem %java_client_dir%\certificate\org1-ca-chain.pem

rem copy /y %chain_dir%\org1\data\admin\msp\keystore\* %java_client_dir%\certificate\admin_sk
rem copy /y %chain_dir%\org1\data\admin\msp\signcerts\* %java_client_dir%\certificate\admin_cert.pem
rem copy /y %chain_dir%\org1\data\admin\tls\client.crt %java_client_dir%\certificate\admin_client.crt
rem copy /y %chain_dir%\org1\data\admin\tls\client.key %java_client_dir%\certificate\admin_client.key

copy /y %chain_dir%\org1\data\user\msp\keystore\* %java_client_dir%\certificate\user_sk
copy /y %chain_dir%\org1\data\user\msp\signcerts\* %java_client_dir%\certificate\user_cert.pem
copy /y %chain_dir%\org1\data\user\tls\client.crt %java_client_dir%\certificate\user_client.crt
copy /y %chain_dir%\org1\data\user\tls\client.key %java_client_dir%\certificate\user_client.key
