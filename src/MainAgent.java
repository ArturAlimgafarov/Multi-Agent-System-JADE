import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
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
    public long _workersCount;
    public long _ordersCount;
    public long _ordersCounter;

    @Override
    protected void setup() {
        registerService();

        addBehaviour(new WaitingFinish());

        _ordersCounter = 0;

        System.out.println(getLocalName() + " launched.");

        // parse agent's data and creating
        var inputFile = (String) getArguments()[0];
        var parser = new JSONParser();
        try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8))) {
            var jsonInput = (JSONObject) parser.parse(reader);

            // workers
            var workers = (JSONArray) jsonInput.get("workers");
            _workersCount = workers.size();
            Map<Integer, Integer> amount = new HashMap<>();

            for (Object o : workers) {
                if (o instanceof JSONObject worker) {
                    String name = (String) worker.get("name");
                    long operationID = (long) worker.get("operationID");
                    long productivity = (long) worker.get("productivity");

                    int id = (int) operationID;

                    if (amount.containsKey(id)) {
                        int k = amount.get(id);
                        amount.put(id, k + 1);
                    }
                    else {
                        amount.put(id, 1);
                    }

                    Object[] workerModel = {
                            operationID, productivity
                    };

                    createAgent(workerModel,
                            "'" + name + "'",
                            "WorkerAgent"
                    );
                }
            }

            // orders
            var orders = (JSONArray) jsonInput.get("orders");
            _ordersCount = orders.size();

            long orderID = 0;
            for (Object o : orders) {
                if (o instanceof JSONObject order) {
                    long operationID = (long) order.get("operationID");
                    long complexity = (long) order.get("complexity");
                    long countWorkers = amount.get((int) operationID);

                    Object[] orderModel = {
                            operationID, complexity, countWorkers, getAID()
                    };

                    createAgent(orderModel,
                            "'Order #" + orderID + "'",
                            "OrderAgent"
                    );

                    orderID++;
                }
            }
        } catch (IOException ignored) {
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

    public class WaitingFinish extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null) {
                switch (msg.getPerformative()) {
                    // message from Order
                    case ACLMessage.INFORM -> {
                        _ordersCounter++;
//                        System.out.println(_ordersCounter + " / " + _ordersCount + " (" + msg.getSender().getLocalName() + ")\n");

                        if (_ordersCounter == _ordersCount) {
                            System.out.println("*** Orders are over. ***");
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void takeDown() {
    }
}
