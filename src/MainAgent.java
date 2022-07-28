import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class MainAgent extends Agent {
    @Override
    protected void setup() {
        registerService();

        System.out.println("MainAgent created");

        Object[] workerModel = {}; // params args
        createAgent(workerModel,
                "Worker #1",
                "WorkerAgent"
        );

        Object[] orderModel = {}; // params args
        createAgent(orderModel,
                "Order #1",
                "OrderAgent"
        );
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
            System.out.println("Error #1");
            e.printStackTrace();
        }

        if (ac != null) {
            try {
                ac.start();
            } catch (StaleProxyException e) {
                System.out.println("Error #2");
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void takeDown() {
    }
}
