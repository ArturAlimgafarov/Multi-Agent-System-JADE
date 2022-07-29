import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class WorkerAgent extends Agent {
    public long _operationID;
    public long _productivity;

    @Override
    protected void setup() {
        Object[] args = getArguments();

        _operationID = (long) args[0];
        _productivity = (long) args[1];

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
                if (msg.getPerformative() == ACLMessage.REQUEST) {
                    var parser = new JSONParser();

                    JSONObject jsonObject = null;
                    try {
                        jsonObject = (JSONObject) parser.parse(msg.getContent());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    long orderOperationID = (long) jsonObject.get("operationID");
                    long orderComplexity = (long) jsonObject.get("complexity");

                    System.out.println(getLocalName() + " receive " + msg.getSender().getLocalName() + " (" + orderOperationID + "; " + orderComplexity + ")");
                }
            }
        }
    }

    @Override
    protected void takeDown() {
    }
}
