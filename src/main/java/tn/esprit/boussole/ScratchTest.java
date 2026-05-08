package tn.esprit.boussole;

import tn.esprit.boussole.service.ChargeService;
import tn.esprit.boussole.models.Charge;
import java.util.List;

public class ScratchTest {
    public static void main(String[] args) {
        try {
            ChargeService service = new ChargeService();
            List<Charge> charges = service.selectAll(null);
            System.out.println("Success! Found " + charges.size() + " charges.");
        } catch (Exception e) {
            System.err.println("Exception occurred:");
            e.printStackTrace();
        }
    }
}
