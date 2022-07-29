import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MainAgent extends Agent {
    @Override
    protected void setup() {
        registerService();

        System.out.println(getLocalName() + " launched.");

        // parse agent's data and creating
        var inputFile = (String) getArguments()[0];
        var parser = new JSONParser();
        try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8))) {
            var jsonInput = (JSONObject) parser.parse(reader);

            int i = 1;
            var orders = (JSONArray) jsonInput.get("orders");
            for (Object o: orders) {
                var order = (JSONObject) o;
                String orderName = "`Order №" + i + "`";
                long orderOperationID = (long) order.get("operationID");
                long complexity = (long) order.get("complexity");

                Object[] orderModel = {orderOperationID, complexity}; // params args
                createAgent(orderModel,
                        orderName,
                        "OrderAgent"
                );

                i++;
            }

            var workers = (JSONArray) jsonInput.get("workers");
            for (Object w: workers) {
                var worker = (JSONObject) w;
                String workerName = "`" + worker.get("name") + "`";
                long workerOperationID = (long) worker.get("operationID");
                long productivity = (long) worker.get("productivity");

                Object[] workerModel = {workerOperationID, productivity}; // params args
                createAgent(workerModel,
                        workerName,
                        "WorkerAgent"
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void registerService() {
        DFAgentDescription dfa = new DFAgentDescription();
        dfa.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("MainAgent");
        sd.setName(getLocalName());
        dfa.addServices(sd);

        try {
            DFService.register(this, dfa);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void createAgent(Object[] agentModel, String name, String agentName) {
        AgentController ac = null;

        try {
            ac = getContainerController().createNewAgent(name, agentName, agentModel);
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }

        if (ac != null) {
            try {
                ac.start();
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void takeDown() {
    }
}
