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
    public String _name;
    public int _operationID;
    public double _productivity;

    @Override
    protected void setup() {
        Object[] args = getArguments();

        _name = (String) args[0];
        _productivity = (double) args[1];

        registerService();

        System.out.println("WorkerAgent " + _name + " created");

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

                    String orderName = (String) jsonObject.get("name");
                    double orderComplexity = (double) jsonObject.get("complexity");

                    System.out.println(_name + " receive " + orderName + " (" + orderComplexity + ")");
                }
            }
        }
    }

    @Override
    protected void takeDown() {
    }
}
