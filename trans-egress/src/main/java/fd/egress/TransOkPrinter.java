package fd.egress;

import akka.stream.javadsl.RunnableGraph;
import cloudflow.akkastream.AkkaStreamlet;
import cloudflow.akkastream.AkkaStreamletLogic;
import cloudflow.akkastream.javadsl.RunnableGraphStreamletLogic;
import cloudflow.streamlets.StreamletShape;
import cloudflow.streamlets.avro.AvroInlet;
import fd.datamodel.CustomerTransaction;

public class TransOkPrinter extends AkkaStreamlet {
    public AvroInlet<CustomerTransaction> in = AvroInlet.create("trans-ok-in",CustomerTransaction.class);

    @Override
    public StreamletShape shape() {
        return StreamletShape.createWithInlets(in);
    }

    @Override
    public AkkaStreamletLogic createLogic() {
        return new RunnableGraphStreamletLogic(getContext()) {
            @Override
            public RunnableGraph<?> createRunnableGraph() {
                return getSourceWithCommittableContext(in)
                        .map(report ->{
                            log().info("OK [customer: {}, trans: {}]",report.getCustomerId(),report.getTransId());
                            return report;
                        })
                        .to(getCommittableSink());
            }
        };
    }
}
