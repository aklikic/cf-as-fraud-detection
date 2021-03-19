package fd;

import akka.actor.Actor;
import akka.actor.ActorSystem;
import akka.grpc.GrpcClientSettings;
import cloudflow.akkastream.javadsl.util.Either;
import fd.datamodel.CustomerTransaction;
import fd.datamodel.CustomerTransactionFraudReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class AsClient {
    private final CustomerFraudDetectionClient client;
    private final Logger log = LoggerFactory.getLogger(AsClient.class);

    public AsClient(ActorSystem system,String serverHost){
        GrpcClientSettings settings = GrpcClientSettings.connectToServiceAt(serverHost, 443, system);
        client = CustomerFraudDetectionClient.create(settings,system);
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

    private Service.AddTransactionCommand toAs(CustomerTransaction trans){
        return Service.AddTransactionCommand.newBuilder()
                .setCustomerId(trans.getCustomerId())
                .setTrans(Service.Transaction.newBuilder()
                        .setTransId(trans.getTransId())
                        .setAmountCents(trans.getAmountCents())
                        .setTimestamp(trans.getTimestamp()))
                .build();
    }
    private CustomerTransactionFraudReport fromAs(CustomerTransaction trans, Service.FraudDetectionReport report){
        return CustomerTransactionFraudReport.newBuilder()
                .setCustomerId(trans.getCustomerId())
                .setTransId(trans.getTransId())
                .setAmountCents(trans.getAmountCents())
                .setIsPotentialFraud(report.getPotentialFraud())
                .setTimestamp(trans.getTimestamp())
                .setRiskScore(report.getRiskScore())
                .build();
    }

    public static void main(String[] args) throws Exception{
        ActorSystem system = ActorSystem.create("test");
        AsClient client = new AsClient(system,"blue-hill-9886.us-east1.apps.akkaserverless.com");
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
