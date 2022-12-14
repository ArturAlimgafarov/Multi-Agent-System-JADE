import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainAgent extends Agent {
    public long _workersCount;
    public long _workersCounter;
    public long _ordersCount;
    public long _ordersCounter;

    public Map<String, LinkedHashMap<String, Integer>> _scheduler;

    @Override
    protected void setup() {
        registerService();

        addBehaviour(new WaitingFinish());

        _ordersCounter = 0;
        _workersCounter = 0;

        _scheduler = new HashMap<>();

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

                        //System.out.println(_ordersCounter + " / " + _ordersCount + " (" + msg.getSender().getLocalName() + ")\n");

                        if (_ordersCounter == _ordersCount) {
                            System.out.println("\n*** Orders are over. ***\n");

                            ACLMessage messageToWorkers = new ACLMessage(ACLMessage.CANCEL);
                            DFAgentDescription tmpWorkers = new DFAgentDescription();
                            ServiceDescription sdWorkers = new ServiceDescription();
                            sdWorkers.setType("WorkerAgent");
                            tmpWorkers.addServices(sdWorkers);

                            try {
                                DFAgentDescription[] res = null;

                                while (res == null || res != null && res.length < 1) {
                                    res = DFService.search(myAgent, tmpWorkers);
                                }

                                for (var worker: res) {
                                    messageToWorkers.addReceiver(worker.getName());
                                }

                                myAgent.send(messageToWorkers);
                            } catch (FIPAException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    // message from Worker
                    case ACLMessage.PROPOSE -> {
                        _workersCounter++;

                        // read Worker's schedule data
                        var parser = new JSONParser();
                        JSONObject jsonFromWorker = null;

                        try {
                            jsonFromWorker = (JSONObject) parser.parse(msg.getContent());
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        String workerName = msg.getSender().getLocalName();
                        LinkedHashMap<String, Integer> orders = new LinkedHashMap<>();
                        for (Object key: jsonFromWorker.keySet()) {
                            long dur = (long) jsonFromWorker.get(key);
                            orders.put((String) key, (int) dur);
                        }

                        _scheduler.put(workerName, orders);

                        if (_workersCounter == _workersCount) {
                            System.out.println("\n*** All Workers reported. ***\n");

                            // JSON data
                            JSONObject jsonWorkers = new JSONObject();

                            // logging schedule
                            int i = 1;
                            for (var keyWorker: _scheduler.keySet()) {
                                System.out.println((i++) + ". " + keyWorker + ":");
                                var schedule = _scheduler.get(keyWorker);
                                JSONObject jsonOrders = new JSONObject();
                                for (var keyOrder : schedule.keySet()) {
                                    var duration = schedule.get(keyOrder);
                                    System.out.println("\t" + keyOrder + " = " + duration);
                                    jsonOrders.put(keyOrder, duration);
                                }
                                jsonWorkers.put(keyWorker, jsonOrders);
                            }

                            // output schedule to file
                            FileWriter file = null;
                            try {
                                file = new FileWriter("output.json");
                                file.write(jsonWorkers.toJSONString());

                            } catch (IOException e) {
                                e.printStackTrace();

                            } finally {

                                try {
                                    file.flush();
                                    file.close();
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }

                            // deleting Main agent
                            try {
                                getContainerController().getAgent(getLocalName()).kill();
                            } catch (ControllerException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void takeDown() {
        System.out.println("\n*** Program completed. ***\n");
        System.out.println(getAID().getLocalName() + " destroyed.");
        System.exit(0);
    }
}
