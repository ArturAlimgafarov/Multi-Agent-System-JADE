import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import org.json.simple.JSONObject;

public class OrderAgent extends Agent {
    public String _name;
    public double _complexity;

    @Override
    protected void setup() {
        Object[] args = getArguments();

        _name = (String) args[0];
        _complexity = (double) args[1];

        registerService();

        System.out.println("OrderAgent " + _name + " created");

        addBehaviour(new SearchWorker());
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

    public class SearchWorker extends OneShotBehaviour {
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
                jsonToWorker.put("name", _name);
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
    }
}
