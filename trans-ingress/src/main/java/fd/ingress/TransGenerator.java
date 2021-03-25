package fd.ingress;

import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Source;
import cloudflow.akkastream.AkkaStreamlet;
import cloudflow.akkastream.AkkaStreamletLogic;
import cloudflow.akkastream.javadsl.RunnableGraphStreamletLogic;
import cloudflow.streamlets.StreamletShape;
import cloudflow.streamlets.avro.AvroOutlet;
import cloudflow.streamlets.proto.ProtoOutlet;
import fd.datamodel.CustomerTransaction;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TransGenerator extends AkkaStreamlet {

    private Duration frequency = Duration.ofSeconds(10);
    public AvroOutlet<CustomerTransaction> out = AvroOutlet.create("trans-out", CustomerTransaction::getTransId,CustomerTransaction.class);

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
                return Source.tick(frequency,frequency,"")
                             .map(t->Helper.customerIds.get(random.nextInt(Helper.customerIds.size())))
                             .map(this::createTrans)
                             .to(getPlainSink(out));
            }

            public CustomerTransaction createTrans(String customerId){
                UUID transId = UUID.randomUUID();
                int amount = 100 * random.nextInt(15);
                log().info("Generating trans [customerId: {},transId:{}]: amount = {}",customerId,transId,amount);
                return CustomerTransaction.newBuilder()
                        .setTransId(transId.toString())
                        .setCustomerId(customerId.toString())
                        .setTimestamp(System.currentTimeMillis())
                        .setAmountCents(amount)
                        .build();
            }
        };
    }


}
