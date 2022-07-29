import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

public class OrderAgent extends Agent {
    public String _name;
    public double _complexity;

    @Override
    protected void setup() {
        Object[] args = getArguments();

        this._name = (String) args[0];
        this._complexity = (double) args[1];

        registerService();

        System.out.println("OrderAgent " + this._name + " created");
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

    @Override
    protected void takeDown() {
    }
}
