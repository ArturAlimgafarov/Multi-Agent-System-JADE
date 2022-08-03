import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.wrapper.ControllerException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.Math.ceil;

public class WorkerAgent extends Agent {
    public long _operationID;
    public long _productivity;
    public long _busyTime;
    public LinkedHashMap<String, Integer> _orders;

    @Override
    protected void setup() {
        Object[] args = getArguments();

        _operationID = (long) args[0];
        _productivity = (long) args[1];
        _busyTime = 0;
        _orders = new LinkedHashMap<>();

        registerService();

        System.out.println(getLocalName() + " is ready.");

        addBehaviour(new TakingOrders());
    }

    public void registerService() {
        DFAgentDescription dfa = new DFAgentDescription();
        dfa.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("WorkerAgent");
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

    public class TakingOrders extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null) {
                switch (msg.getPerformative()) {
                    case ACLMessage.REQUEST -> {
                        var parser = new JSONParser();
                        JSONObject jsonObject = null;

                        try {
                            jsonObject = (JSONObject) parser.parse(msg.getContent());
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        long orderOperationID = (long) jsonObject.get("operationID");
                        long orderComplexity = (long) jsonObject.get("complexity");

                        if (orderOperationID == _operationID) {
                            ACLMessage messageToOrder = new ACLMessage(ACLMessage.PROPOSE);

                            long duration = (long) ceil((double) orderComplexity / (double) _productivity);

                            JSONObject jsonToOrder = new JSONObject();
                            jsonToOrder.put("name", getLocalName());
                            jsonToOrder.put("duration", duration);
                            jsonToOrder.put("busy", _busyTime);

                            messageToOrder.setContent(jsonToOrder.toJSONString());
                            messageToOrder.addReceiver(msg.getSender());

                            myAgent.send(messageToOrder);
                        }
                    }

                    case ACLMessage.AGREE -> {
                        var parser1 = new JSONParser();
                        JSONObject jsonFromOrder = null;

                        try {
                            jsonFromOrder = (JSONObject) parser1.parse(msg.getContent());
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        String orderName = msg.getSender().getLocalName();
                        long busy = (long) jsonFromOrder.get("busy");
                        long duration = (long) jsonFromOrder.get("duration");
                        ACLMessage responseToOrder;
                        if (_busyTime > busy) {
                            // this worker is already busy
                            responseToOrder = new ACLMessage(ACLMessage.REFUSE);
                            responseToOrder.addReceiver(msg.getSender());

                            myAgent.send(responseToOrder);

                            System.out.println(getLocalName() + " REFUSED " + orderName);
                        } else {
                            responseToOrder = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                            responseToOrder.addReceiver(msg.getSender());

                            myAgent.send(responseToOrder);

                            System.out.println(getLocalName() + " ACCEPTED " + orderName);

                            _orders.put(orderName, (int) duration);

                            _busyTime += duration;
                        }
                    }

                    // message from Main about orders are over
                    case ACLMessage.CANCEL -> {
                        long sum = 0;
                        for (var key: _orders.keySet()) {
                            sum += _orders.get(key);
                        }

                        System.out.println(getLocalName() + ": " + _orders + " (" + sum + ").");

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

    @Override
    protected void takeDown() {
        System.out.println(getAID().getLocalName() + " destroyed.");
    }
}
