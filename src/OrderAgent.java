import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.wrapper.ControllerException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.HashMap;
import java.util.Map;

public class OrderAgent extends Agent {
    public long _operationID;
    public long _complexity;
    public long _availableCount;
    public AID _mainAID;
    public Map<String, Map<String, Long>> _variants;

    @Override
    protected void setup() {
        Object[] args = getArguments();

        _operationID = (long) args[0];
        _complexity = (long) args[1];
        _availableCount = (long) args[2];
        _mainAID = (AID) args[3];
        _variants = new HashMap<>();

        registerService();

        System.out.println(getLocalName() + " created.");

        addBehaviour(new StartSearchingWorker());
        addBehaviour(new WaitingFreeWorker());
    }

    public void registerService() {
        DFAgentDescription dfa = new DFAgentDescription();
        dfa.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("OrderAgent");
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

    public class WaitingFreeWorker extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null) {
                switch (msg.getPerformative()) {
                    case ACLMessage.PROPOSE -> {
                        var parser = new JSONParser();
                        JSONObject jsonObject = null;

                        try {
                            jsonObject = (JSONObject) parser.parse(msg.getContent());
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        HashMap<String, Long> times = new HashMap<>();
                        times.put("duration", (long) jsonObject.get("duration"));
                        times.put("busy", (long) jsonObject.get("busy"));
                        _variants.put((String) jsonObject.get("name"), times);

                        if (_variants.size() == _availableCount) {
                            // choose best variant
                            String workerName = _variants.keySet().toArray()[0].toString();
                            long duration = _variants.get(workerName).get("duration");
                            long busy = _variants.get(workerName).get("busy");
                            long sumTime = duration + busy;

                            for (var name : _variants.keySet()) {
                                if (_variants.get(name).get("duration") + _variants.get(name).get("busy") < sumTime) {
                                    duration = _variants.get(name).get("duration");
                                    workerName = name;
                                    sumTime = duration + _variants.get(name).get("busy");
                                }
                            }

                            ACLMessage messageToWorker = new ACLMessage(ACLMessage.AGREE);

                            JSONObject jsonToWorker = new JSONObject();

                            busy = _variants.get(workerName).get("busy");
                            jsonToWorker.put("busy", busy);
                            jsonToWorker.put("duration", duration);

                            messageToWorker.setContent(jsonToWorker.toJSONString());
                            messageToWorker.addReceiver(getAID(workerName));

                            myAgent.send(messageToWorker);
                        }
                    }

                    case ACLMessage.REFUSE -> {
                        ACLMessage messageToWorkers = new ACLMessage(ACLMessage.REQUEST);
                        DFAgentDescription tmpWorkers = new DFAgentDescription();
                        ServiceDescription sdWorkers = new ServiceDescription();
                        sdWorkers.setType("Worker");
                        tmpWorkers.addServices(sdWorkers);

                        try {
                            DFAgentDescription[] res = null;

                            while (res == null || res != null && res.length < 1) {
                                res = DFService.search(myAgent, tmpWorkers);
                            }

                            JSONObject jsonToWorker = new JSONObject();
                            jsonToWorker.put("operationID", _operationID);
                            jsonToWorker.put("complexity", _complexity);

                            messageToWorkers.setContent(jsonToWorker.toJSONString());

                            for (var worker : res) {
                                messageToWorkers.addReceiver(worker.getName());
                            }

                            myAgent.send(messageToWorkers);
                        } catch (FIPAException e) {
                            e.printStackTrace();
                        }
                    }

                    case ACLMessage.ACCEPT_PROPOSAL -> {
                        ACLMessage messageToMain = new ACLMessage(ACLMessage.INFORM);
                        messageToMain.addReceiver(_mainAID);
                        myAgent.send(messageToMain);

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

    public class StartSearchingWorker extends OneShotBehaviour {
        @Override
        public void action() {
            ACLMessage messageToWorkers = new ACLMessage(ACLMessage.REQUEST);
            DFAgentDescription tmpWorkers = new DFAgentDescription();
            ServiceDescription sdWorkers = new ServiceDescription();
            sdWorkers.setType("WorkerAgent");
            tmpWorkers.addServices(sdWorkers);

            try {
                DFAgentDescription[] res = null;

                while (res == null || res != null && res.length < 1) {
                    res = DFService.search(myAgent, tmpWorkers);
                }

                JSONObject jsonToWorker = new JSONObject();
                jsonToWorker.put("operationID", _operationID);
                jsonToWorker.put("complexity", _complexity);

                messageToWorkers.setContent(jsonToWorker.toJSONString());

                for (var worker: res) {
                    messageToWorkers.addReceiver(worker.getName());
                }

                myAgent.send(messageToWorkers);
            } catch (FIPAException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void takeDown() {
        System.out.println(getAID().getLocalName() + " destroyed.");
    }
}
