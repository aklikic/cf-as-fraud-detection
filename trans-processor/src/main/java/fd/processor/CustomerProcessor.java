package fd.processor;

import akka.Done;
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
import fd.datamodel.Customer;
import fd.datamodel.CustomerTransaction;
import fd.datamodel.CustomerTransactionFraudReport;

public class CustomerProcessor extends AkkaStreamlet {

    private final int parallelism = 1;

    public AvroInlet<Customer> in = AvroInlet.create("customer-in", Customer.class);

    @Override
    public StreamletShape shape() {
        return StreamletShape.createWithInlets(in);
    }
    @Override
    public AkkaStreamletLogic createLogic() {
        return new RunnableGraphStreamletLogic(getContext()) {

            private final AsClient asClient = new AsClient(getContext().system());
            private FlowWithContext<Customer, ConsumerMessage.Committable, Done, ConsumerMessage.Committable, NotUsed> customerProcessFLow =
                   FlowWithCommittableContext.<Customer>create()
                            .map(customer->{
                                log().info("Processing customer [customerId: {}]",customer.getCustomerId());
                                return customer;
                            })
                            .mapAsync(parallelism, asClient::createFraudDetectionForCustomer);

            @Override
            public RunnableGraph<?> createRunnableGraph() {
                return getSourceWithCommittableContext(in).via(customerProcessFLow).to(getCommittableSink());
            }
        };
    }
}
