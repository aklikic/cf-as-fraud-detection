package fd;

import akka.Done;
import akka.actor.ActorSystem;
import akka.grpc.GrpcClientSettings;
import cloudflow.akkastream.javadsl.util.Either;
import fd.datamodel.Customer;
import fd.datamodel.CustomerTransaction;
import fd.datamodel.CustomerTransactionFraudReport;
import frauddetection.FraudDetectionCommon;
import frauddetection.FraudDetectionService;
import frauddetection.FraudDetectionServiceClient;
import frauddetection.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class AsClient {
    private final static String serverHost = "bitter-queen-7098.us-east1.apps.akkaserverless.com";
    private final FraudDetectionService client;
    private final Logger log = LoggerFactory.getLogger(AsClient.class);

    public AsClient(ActorSystem system){
        GrpcClientSettings settings = GrpcClientSettings.connectToServiceAt(serverHost, 443, system);
        client = FraudDetectionServiceClient.create(settings,system);
    }

    public CompletionStage<Either<CustomerTransaction, CustomerTransactionFraudReport>> addTransaction(CustomerTransaction trans){

        CompletionStage<Either<CustomerTransaction, CustomerTransactionFraudReport>> cs =
                client.addTransaction(toAs(trans))
                        .thenApply(report -> fromAs(trans,report))
                        .thenApply(report -> {
                            if(report.getIsPotentialFraud())
                                return Either.right(report);
                            else
                                return Either.left(trans);
                        });

        cs=cs.exceptionally(e->{
            log.error("Trans [{}] error: {}",trans.getCustomerId(),trans.getTransId(),e);
            return Either.left(trans);
        });
        return cs;
    }

    public CompletionStage<Done> createFraudDetectionForCustomer(Customer customer){
        return client.createFraudDetection(toAs(customer))
                     .thenApply(e->Done.getInstance());
    }


    private Service.AddTransactionCommand toAs(CustomerTransaction trans){
        return Service.AddTransactionCommand.newBuilder()
                .setCustomerId(trans.getCustomerId())
                .setTransactionId(trans.getTransId())
                .setAmountCents(trans.getAmountCents())
                .setTimestamp(trans.getTimestamp())
                .build();
    }
    private CustomerTransactionFraudReport fromAs(CustomerTransaction trans, FraudDetectionCommon.ScoredTransactionState report){
        return CustomerTransactionFraudReport.newBuilder()
                .setCustomerId(trans.getCustomerId())
                .setTransId(trans.getTransId())
                .setAmountCents(trans.getAmountCents())
                .setIsPotentialFraud(report.getPotentialFraud())
                .setTimestamp(trans.getTimestamp())
                .setRiskScore(report.getRiskScore())
                .build();
    }

    private static Service.CreateFraudDetectionCommand toAs(Customer customer){
        return Service.CreateFraudDetectionCommand.newBuilder()
                .setCustomerId(customer.getCustomerId())
                .setRuleId(customer.getRuleId())
                .setMaxAmountCents(customer.getMaxAmountCents())
                .build();
    }

    public static void main(String[] args) throws Exception{
        ActorSystem system = ActorSystem.create("test");
        AsClient client = new AsClient(system);
        Either<CustomerTransaction, CustomerTransactionFraudReport> res =
        client.addTransaction(CustomerTransaction.newBuilder()
                .setAmountCents(1001)
                .setTimestamp(System.currentTimeMillis())
                .setCustomerId("11051")
                .setTransId(UUID.randomUUID().toString())
                .build()).toCompletableFuture().get(3, TimeUnit.SECONDS);
        System.out.println(res.isRight());

    }



}
