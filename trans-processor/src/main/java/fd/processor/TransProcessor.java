package fd.processor;

import akka.NotUsed;
import akka.kafka.ConsumerMessage;
import akka.stream.javadsl.FlowWithContext;
import akka.stream.javadsl.RunnableGraph;
import cloudflow.akkastream.AkkaStreamlet;
import cloudflow.akkastream.AkkaStreamletLogic;
import cloudflow.akkastream.javadsl.FlowWithCommittableContext;
import cloudflow.akkastream.javadsl.RunnableGraphStreamletLogic;
import cloudflow.akkastream.javadsl.util.Either;
import cloudflow.akkastream.util.javadsl.Splitter;
import cloudflow.streamlets.StreamletShape;
import cloudflow.streamlets.avro.AvroInlet;
import cloudflow.streamlets.avro.AvroOutlet;
import fd.AsClient;
import fd.datamodel.CustomerTransaction;
import fd.datamodel.CustomerTransactionFraudReport;

public class TransProcessor extends AkkaStreamlet {

    private final int parallelism = 1;

    public AvroInlet<CustomerTransaction> in = AvroInlet.create("trans-in",CustomerTransaction.class);
    public AvroOutlet<CustomerTransactionFraudReport> outRisk = AvroOutlet.create("trans-risk-out",msg->msg.getCustomerId(),CustomerTransactionFraudReport.class);
    public AvroOutlet<CustomerTransaction> outOk = AvroOutlet.create("trans-ok-out",msg->msg.getCustomerId(),CustomerTransaction.class);

    @Override
    public StreamletShape shape() {
        return StreamletShape.createWithInlets(in)
                             .withOutlets(outOk, outRisk);
    }
    @Override
    public AkkaStreamletLogic createLogic() {
        return new RunnableGraphStreamletLogic(getContext()) {

            private final AsClient asClient = new AsClient(getContext().system());
            private FlowWithContext<CustomerTransaction, ConsumerMessage.Committable, Either<CustomerTransaction,CustomerTransactionFraudReport>, ConsumerMessage.Committable, NotUsed> transProcessFLow =
                    FlowWithCommittableContext.<CustomerTransaction>create()
                            .map(trans->{
                                log().info("Processing trans [customerId: {};transId: {}]",trans.getCustomerId(),trans.getTransId());
                                return trans;
                            })
                            .mapAsync(parallelism, asClient::addTransaction);

            @Override
            public RunnableGraph<?> createRunnableGraph() {
                return getSourceWithCommittableContext(in).to(Splitter.sink(transProcessFLow,outOk,outRisk,getContext()));
            }
        };
    }
}
