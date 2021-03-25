package fd.ingress;

import akka.stream.javadsl.Concat;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Source;
import cloudflow.akkastream.AkkaStreamlet;
import cloudflow.akkastream.AkkaStreamletLogic;
import cloudflow.akkastream.javadsl.RunnableGraphStreamletLogic;
import cloudflow.akkastream.util.javadsl.HttpServerLogic;
import cloudflow.streamlets.StreamletShape;
import cloudflow.streamlets.avro.AvroOutlet;
import fd.datamodel.Customer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Random;
import java.util.stream.Collectors;

public class CustomerImporter extends AkkaStreamlet {

    private Duration frequency = Duration.ofSeconds(60);
    public AvroOutlet<Customer> out = AvroOutlet.create("customer-out", Customer::getCustomerId,Customer.class);
    private Random random = new Random();
    @Override
    public StreamletShape shape() {
        return StreamletShape.createWithOutlets(out);
    }

    @Override
    public AkkaStreamletLogic createLogic() {
        return new RunnableGraphStreamletLogic(getContext()) {
            @Override
            public RunnableGraph<?> createRunnableGraph() {
                Source<Customer,?> initialSource = Source.from(Helper.customerIds.stream().map(this::createCustomer).collect(Collectors.toList()));
                Source<Customer,?> tickSource =  Source.tick(frequency,frequency,"")
                                                            .map(t->Helper.customerIds.get(random.nextInt(Helper.customerIds.size())))
                                                            .map(this::createCustomer);
                return initialSource.concat(tickSource).to(getPlainSink(out));


            }

            public Customer createCustomer(String customerId){

                log().info("Importing/Updating customer [customerId: {}]",customerId);
                return Customer.newBuilder()
                        .setCustomerId(customerId)
                        .setRuleId(Helper.ruleId)
                        .setMaxAmountCents(Helper.maxAmountCents)
                        .build();
            }
        };
    }


}
