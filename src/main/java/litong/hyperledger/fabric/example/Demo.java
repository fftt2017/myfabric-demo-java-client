package litong.hyperledger.fabric.example;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hyperledger.fabric.sdk.BlockInfo.EnvelopeType.TRANSACTION_ENVELOPE;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
import org.hyperledger.fabric.protos.ledger.rwset.kvrwset.KvRwset;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.BlockEvent.TransactionEvent;
import org.hyperledger.fabric.sdk.BlockInfo;
import org.hyperledger.fabric.sdk.BlockchainInfo;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.ChaincodeResponse.Status;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.SDKUtils;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.TxReadWriteSetInfo;
import org.hyperledger.fabric.sdk.TxReadWriteSetInfo.NsRwsetInfo;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.InvalidProtocolBufferRuntimeException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;

import com.google.common.io.Files;
import com.google.protobuf.InvalidProtocolBufferException;

public class Demo {
	private static final Logger log = Logger.getLogger(Demo.class);
	
	private static final Charset CHARSET = Charset.forName("UTF-8");
	private static final String HOST = "192.168.99.101";
	private static final String CHANNEL_NAME = "demo-app-chain";
	private static final String CHAIN_CODE = "mycc";
	
	private static Peer peer1;
	private static Orderer orderer;	
	private static HFClient client;
	
	public static void main(String[] args) throws Exception {
		Channel channel = getDemoChannel();
		channel.initialize();
		
		log.info("call query method starts......");
		query(client, channel, CHAIN_CODE, "query", new String[] {"a"});
		log.info("call query method ends......");
		
		log.info("call invoke method starts......");
		invoke(client, channel, CHAIN_CODE, "invoke", new String[] {"a", "b", "10"});
		log.info("call invoke method ends......");
		
		log.info("call query method starts......");
		query(client, channel, CHAIN_CODE, "query", new String[] {"a"});
		log.info("call query method ends......");
		
		log.info("explore blocks starts......");
		blockWalker(client, channel);
		log.info("explore blocks ends......");
		
		System.out.println("done!");
	}
	
	private static Channel getDemoChannel() throws Exception {
		SampleEnrollment enrollment = loadEnrollment("certificate/user_cert.pem", "certificate/user_sk");
		SampleUser user = new SampleUser("user", "org1MSP", "org1", enrollment);
		
		client = HFClient.createNewInstance();
		client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
		client.setUserContext(user);
		
		Channel channel = client.newChannel(CHANNEL_NAME);
		orderer = newOrderer(client, "orderer", "certificate/org0-ca-chain.pem", "certificate/user_client.key", "certificate/user_client.crt", "orderer1-org0", "grpcs://" + HOST + ":7050");
		channel.addOrderer(orderer);
		
		peer1 = newPeer(client, "peer1", "certificate/org1-ca-chain.pem", "certificate/user_client.key", "certificate/user_client.crt", "peer1-org1", "grpcs://" + HOST + ":7051");
		channel.addPeer(peer1);
		
		return channel;
	}
	
	private static Orderer newOrderer(HFClient client, String name, String pemFile, String clientTLSKeyFile, String clientTLSCertFile, String hostName, String url) throws InvalidArgumentException {
		Properties orderProps = new Properties();
		orderProps.setProperty("pemFile", pemFile);
		orderProps.setProperty("sslProvider", "openSSL");
		orderProps.setProperty("negotiationType", "TLS");
		orderProps.setProperty("ordererWaitTimeMilliSecs", "300000");
		orderProps.setProperty("hostnameOverride", hostName);
		orderProps.setProperty("clientKeyFile", clientTLSKeyFile);
		orderProps.setProperty("clientCertFile", clientTLSCertFile);
		
		return client.newOrderer(name, url, orderProps);
	}
	
	private static Peer newPeer(HFClient client, String name, String pemFile, String clientTLSKeyFile, String clientTLSCertFile, String hostName, String url) throws InvalidArgumentException {
		Properties peerProps = new Properties();
		peerProps.setProperty("pemFile", pemFile);
		peerProps.setProperty("sslProvider", "openSSL");
		peerProps.setProperty("negotiationType", "TLS");
		peerProps.setProperty("hostnameOverride", hostName);
		peerProps.setProperty("clientKeyFile", clientTLSKeyFile);
		peerProps.setProperty("clientCertFile", clientTLSCertFile);
		
		return client.newPeer(name, url, peerProps);
	}
	
	private static void query(HFClient client, Channel channel, String chainCode, String fcn, String[] parameters) throws Exception {
        QueryByChaincodeRequest req = client.newQueryProposalRequest();
        ChaincodeID cid = ChaincodeID.newBuilder().setName(chainCode).build();
        req.setChaincodeID(cid);
        req.setFcn(fcn);
        req.setArgs(parameters);
        
        List<Peer> peers = new ArrayList<>();
        peers.add(peer1);
        
        Collection<ProposalResponse> resps = channel.queryByChaincode(req, peers);
        for (ProposalResponse resp : resps) {
            String payload = new String(resp.getChaincodeActionResponsePayload());
            System.out.println("response: " + payload);
        }
    }
	
	private static void invoke(HFClient client, Channel channel, String chainCode, String fcn, String[] parameters) throws Exception {
		TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
        ChaincodeID cid = ChaincodeID.newBuilder().setName(chainCode).build();
        transactionProposalRequest.setChaincodeID(cid);
        transactionProposalRequest.setFcn(fcn);
        transactionProposalRequest.setArgs(parameters);
		
        List<Peer> peers = new ArrayList<>();
        peers.add(peer1);
        //peers.add(peer2);
        
        Collection<ProposalResponse> invokePropResp = channel.sendTransactionProposal(transactionProposalRequest);
        for (ProposalResponse response : invokePropResp) {
            if (response.getStatus() == Status.SUCCESS) {
                out("Proposal response SUCC Txid=%s, peer=%s, data=[%s]", response.getTransactionID(), response.getPeer(), new String(response.getChaincodeActionResponsePayload()));
                dumpRWSet(response);
            } else {
                out("Proposal response FAIL Txid=%s, peer=%s, data=[%s]", response.getTransactionID(), response.getPeer(), new String(response.getChaincodeActionResponsePayload()));
            }
        }
        out("Sending to orderer");
        
        sendTransaction(channel, invokePropResp);
	}
	
    private static void sendTransaction(Channel channel, Collection<ProposalResponse> invokePropResp) throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<BlockEvent.TransactionEvent> carfuture = channel.sendTransaction(invokePropResp);
        out("Wait event");
        TransactionEvent transactionEvent = carfuture.get(30, TimeUnit.SECONDS);
        out("Wait event return: " + transactionEvent.getChannelId() + " " + transactionEvent.getTransactionID() + " " + transactionEvent.getType() + " " + transactionEvent.getValidationCode());
    }
	
	private static void out(String format, Object... args) {
        log.info(String.format(format, args));
    }
	
	private static void dumpRWSet(ProposalResponse response) {
        try {
            for (NsRwsetInfo nsRwsetInfo : response.getChaincodeActionResponseReadWriteSetInfo().getNsRwsetInfos()) {
                String namespace = nsRwsetInfo.getNamespace();
                KvRwset.KVRWSet rws = nsRwsetInfo.getRwset();

                int rsid = -1;
                for (KvRwset.KVRead readList : rws.getReadsList()) {
                    rsid++;
                    out("Namespace %s read  set[%d]: key[%s]=version[%d:%d]", namespace, rsid, readList.getKey(), readList.getVersion().getBlockNum(), readList.getVersion().getTxNum());
                }

                rsid = -1;
                for (KvRwset.KVWrite writeList : rws.getWritesList()) {
                    rsid++;
                    String valAsString = printableString(new String(writeList.getValue().toByteArray(), "UTF-8"));
                    out("Namespace %s write set[%d]: key[%s]=value[%s]", namespace, rsid, writeList.getKey(), valAsString);
                }
            }
        } catch (InvalidArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidProtocolBufferException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private static String printableString(final String string) {
        int maxLogStringLength = 64;
        if (string == null || string.length() == 0) {
            return string;
        }

        String ret = string.replaceAll("[^\\p{Print}]", "?");
        ret = ret.substring(0, Math.min(ret.length(), maxLogStringLength)) + (ret.length() > maxLogStringLength ? "..." : "");
        return ret;
    }
	
	private static SampleEnrollment loadEnrollment(String certFile, String privateKeyFile) throws Exception {
		String cert = getCert(certFile);
		PrivateKey privateKey = getPemPrivateKey(privateKeyFile);
		
		return new SampleEnrollment(cert, privateKey);
	}
	
	private static String getCert(String certFile) throws IOException {
		File f = new File(certFile);
		String publicKeyPEM = Files.asCharSource(f, CHARSET).read();
	    return publicKeyPEM;
	}
	
	private static PrivateKey getPemPrivateKey(String privateKeyFile) throws Exception {
		File f = new File(privateKeyFile);
		String privKeyPEM = Files.asCharSource(f, CHARSET).read();
	
		privKeyPEM = privKeyPEM.replace("-----BEGIN PRIVATE KEY-----", "");
		privKeyPEM = privKeyPEM.replace("-----END PRIVATE KEY-----", "");
	    log.info("privateKey: " + privKeyPEM);
	
	    Base64 b64 = new Base64();
	    byte [] decoded = b64.decode(privKeyPEM);
	
	    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
	    KeyFactory kf = KeyFactory.getInstance("EC");
	    return kf.generatePrivate(spec);
	}
	
	private static void blockWalker(HFClient client, Channel channel) throws InvalidArgumentException, ProposalException, IOException {
        try {
            BlockchainInfo channelInfo = channel.queryBlockchainInfo();
            out("block chain height: %d", channelInfo.getHeight());
            out("current block hash: %s", Hex.encodeHexString(channelInfo.getCurrentBlockHash()));
            out("previous block hash: %s", Hex.encodeHexString(channelInfo.getPreviousBlockHash()));

            for (long current = channelInfo.getHeight() - 1; current > -1; --current) {
                BlockInfo returnedBlock = channel.queryBlockByNumber(current);
                final long blockNumber = returnedBlock.getBlockNumber();

                out("current block number %d has data hash: %s", blockNumber, Hex.encodeHexString(returnedBlock.getDataHash()));
                out("current block number %d has previous hash id: %s", blockNumber, Hex.encodeHexString(returnedBlock.getPreviousHash()));
                out("current block number %d has calculated block hash is %s", blockNumber, Hex.encodeHexString(SDKUtils.calculateBlockHash(client,
                        blockNumber, returnedBlock.getPreviousHash(), returnedBlock.getDataHash())));

                out("current block number %d has %d envelope count:", blockNumber, returnedBlock.getEnvelopeCount());
                int i = 0;
                for (BlockInfo.EnvelopeInfo envelopeInfo : returnedBlock.getEnvelopeInfos()) {
                    ++i;

                    out("  Transaction number %d has transaction id: %s", i, envelopeInfo.getTransactionID());
                    final String channelId = envelopeInfo.getChannelId();

                    out("  Transaction number %d has channel id: %s", i, channelId);
                    out("  Transaction number %d has epoch: %d", i, envelopeInfo.getEpoch());
                    out("  Transaction number %d has transaction timestamp: %tB %<te,  %<tY  %<tT %<Tp", i, envelopeInfo.getTimestamp());
                    out("  Transaction number %d has type id: %s", i, "" + envelopeInfo.getType());
                    out("  Transaction number %d has nonce : %s", i, "" + Hex.encodeHexString(envelopeInfo.getNonce()));
                    out("  Transaction number %d has submitter mspid: %s,  certificate: %s", i, envelopeInfo.getCreator().getMspid(), envelopeInfo.getCreator().getId());

                    if (envelopeInfo.getType() == TRANSACTION_ENVELOPE) {
                        BlockInfo.TransactionEnvelopeInfo transactionEnvelopeInfo = (BlockInfo.TransactionEnvelopeInfo) envelopeInfo;

                        out("  Transaction number %d has %d actions", i, transactionEnvelopeInfo.getTransactionActionInfoCount());
                        out("  Transaction number %d isValid %b", i, transactionEnvelopeInfo.isValid());
                        out("  Transaction number %d validation code %d", i, transactionEnvelopeInfo.getValidationCode());

                        int j = 0;
                        for (BlockInfo.TransactionEnvelopeInfo.TransactionActionInfo transactionActionInfo : transactionEnvelopeInfo.getTransactionActionInfos()) {
                            ++j;
                            out("   Transaction action %d has response status %d", j, transactionActionInfo.getResponseStatus());

                            out("   Transaction action %d has response message bytes as string: %s", j,
                                    printableString(new String(transactionActionInfo.getResponseMessageBytes(), UTF_8)));
                            out("   Transaction action %d has %d endorsements", j, transactionActionInfo.getEndorsementsCount());

                            for (int n = 0; n < transactionActionInfo.getEndorsementsCount(); ++n) {
                                BlockInfo.EndorserInfo endorserInfo = transactionActionInfo.getEndorsementInfo(n);
                                out("Endorser %d signature: %s", n, Hex.encodeHexString(endorserInfo.getSignature()));
                                out("Endorser %d endorser: mspid %s \n certificate %s", n, endorserInfo.getMspid(), endorserInfo.getId());
                            }
                            out("   Transaction action %d has %d chaincode input arguments", j, transactionActionInfo.getChaincodeInputArgsCount());
                            for (int z = 0; z < transactionActionInfo.getChaincodeInputArgsCount(); ++z) {
                                out("     Transaction action %d has chaincode input argument %d is: %s", j, z,
                                        printableString(new String(transactionActionInfo.getChaincodeInputArgs(z), UTF_8)));
                            }

                            out("   Transaction action %d proposal response status: %d", j,
                                    transactionActionInfo.getProposalResponseStatus());
                            out("   Transaction action %d proposal response payload: %s", j,
                                    printableString(new String(transactionActionInfo.getProposalResponsePayload())));

                            String chaincodeIDName = transactionActionInfo.getChaincodeIDName();
                            String chaincodeIDVersion = transactionActionInfo.getChaincodeIDVersion();
                            String chaincodeIDPath = transactionActionInfo.getChaincodeIDPath();
                            out("   Transaction action %d proposal chaincodeIDName: %s, chaincodeIDVersion: %s,  chaincodeIDPath: %s ", j,
                                    chaincodeIDName, chaincodeIDVersion, chaincodeIDPath);

                            TxReadWriteSetInfo rwsetInfo = transactionActionInfo.getTxReadWriteSet();
                            if (null != rwsetInfo) {
                                out("   Transaction action %d has %d name space read write sets", j, rwsetInfo.getNsRwsetCount());

                                for (TxReadWriteSetInfo.NsRwsetInfo nsRwsetInfo : rwsetInfo.getNsRwsetInfos()) {
                                    final String namespace = nsRwsetInfo.getNamespace();
                                    KvRwset.KVRWSet rws = nsRwsetInfo.getRwset();

                                    int rs = -1;
                                    for (KvRwset.KVRead readList : rws.getReadsList()) {
                                        rs++;
                                        out("     Namespace %s read set %d key %s  version [%d:%d]", namespace, rs, readList.getKey(),
                                                readList.getVersion().getBlockNum(), readList.getVersion().getTxNum());
                                    }

                                    rs = -1;
                                    for (KvRwset.KVWrite writeList : rws.getWritesList()) {
                                        rs++;
                                        String valAsString = printableString(new String(writeList.getValue().toByteArray(), UTF_8));

                                        out("     Namespace %s write set %d key %s has value '%s' ", namespace, rs,
                                                writeList.getKey(),
                                                valAsString);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (InvalidProtocolBufferRuntimeException e) {
            throw e.getCause();
        }
    }
}
