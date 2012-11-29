import play.*;
import play.jobs.*;
import play.test.*;
 
import models.*;
 
@OnApplicationStart
public class Bootstrap extends Job {
 
    public void doJob() {
        // Check if the database is empty
        //User user = User.all().first();
        User.em().createQuery("DELETE FROM User u").executeUpdate();

        if(User.count() == 0) {
            Fixtures.loadModels("initial-data.yml");
            System.out.println("HERE");
        } else {
            System.out.println("There are users");
        }
    }
 
}