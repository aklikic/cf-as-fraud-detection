package fd.egress;

import akka.stream.javadsl.RunnableGraph;
import cloudflow.akkastream.AkkaStreamlet;
import cloudflow.akkastream.AkkaStreamletLogic;
import cloudflow.akkastream.javadsl.RunnableGraphStreamletLogic;
import cloudflow.streamlets.StreamletShape;
import cloudflow.streamlets.avro.AvroInlet;
import fd.datamodel.CustomerTransactionFraudReport;

public class TransFraudRiskPrinter extends AkkaStreamlet {
    public AvroInlet<CustomerTransactionFraudReport> in = AvroInlet.create("trans-risk-in",CustomerTransactionFraudReport.class);

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
                            log().error("POTENTIAL FRAUD!!! [customer: {}, trans: {}]: risk score = {}",report.getCustomerId(),report.getTransId(),report.getRiskScore());
                            return report;
                        })
                        .to(getCommittableSink());
            }
        };
    }
}
