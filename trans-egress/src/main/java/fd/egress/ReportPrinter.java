package fd.egress;

import akka.stream.javadsl.RunnableGraph;
import cloudflow.akkastream.AkkaStreamlet;
import cloudflow.akkastream.AkkaStreamletLogic;
import cloudflow.akkastream.javadsl.RunnableGraphStreamletLogic;
import cloudflow.streamlets.StreamletShape;
import cloudflow.streamlets.avro.AvroInlet;
import fd.datamodel.CustomerTransactionFraudReport;

public class ReportPrinter extends AkkaStreamlet {
    public AvroInlet<CustomerTransactionFraudReport> in = AvroInlet.create("report-in",CustomerTransactionFraudReport.class);

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
                            if(report.getIsPotentialFraud())
                                log().error("POTENTIAL FRAUD!!! [customer: {}, trans: {}]: risk score = {}",report.getCustomerId(),report.getTransId(),report.getRiskScore());
                            else
                                log().info("OK [customer: {}, trans: {}]",report.getCustomerId(),report.getTransId());
                            return report;
                        })
                        .to(getCommittableSink());
            }
        };
    }
}
