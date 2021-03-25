package fd.ingress;

import akka.http.javadsl.marshallers.jackson.Jackson;
import cloudflow.akkastream.AkkaServerStreamlet;
import cloudflow.akkastream.AkkaStreamletLogic;
import cloudflow.akkastream.util.javadsl.HttpServerLogic;
import cloudflow.streamlets.RoundRobinPartitioner;
import cloudflow.streamlets.StreamletShape;
import cloudflow.streamlets.avro.AvroOutlet;
import fd.datamodel.Customer;

import java.time.Duration;

public class HttpCustomerImporter extends AkkaServerStreamlet {

    public AvroOutlet<Customer> out = AvroOutlet.create("customer-out", Customer::getCustomerId,Customer.class);

    public StreamletShape shape() {
        return StreamletShape.createWithOutlets(out);
    }

    public AkkaStreamletLogic createLogic() {

        return HttpServerLogic.createDefault(
                this, out, Jackson.byteStringUnmarshaller(Customer.class), getContext());
    }
}
