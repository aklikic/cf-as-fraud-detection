package fd.ingress;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Helper {
    public static final List<String> customerIds;
    public static final String ruleId = "1";
    public static final int maxAmountCents = 1000;

    static {
        customerIds = IntStream.range(0,10)
                               .mapToObj(i->"0511"+i)
                               .collect(Collectors.toList());
    }
}
