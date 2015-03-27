package org.opentripplanner.profile;

/**
 * Represents the strategy a user uses in deciding among options in profile analyst.
 */
public enum ProfileChoiceStrategy {
    /**
     * The user has perfect information about the current and all future states of the system (at least until
     * arrival at the destination), and always chooses the optimal route. This is a fair assumption in scheduled
     * systems where the user is using a trip planner (ignoring schedule deviation), and is approximated in systems
     * with good real-time information, especially when transfers are rare (as the number of transfers on a trip
     * approaches zero, the system gets closer to the perfect information case).
     * 
     * This strategy most closely approximates taken by Owen and Levinson 2014 (http://www.its.umn.edu/Publications/ResearchReports/pdfdownloadl.pl?id=2504)
     * in their research at the Minnesota Accessibility Observatory. The main difference is that Owen and Levinson's work
     * does not consider variation in wait times for frequency based services, a relatively minor complaint given that
     * they were working in a US context where vehicles are almost universally scheduled.
     */
    PERFECT_INFORMATION,
    
    /**
     * The risk averse traveler chooses an option that minimizes their upper bound.
     * 
     * While it might seem that travelers would work to minimize their expected value, this seems to require
     * combinatorical combinations; when making a choice, one has to consider the expected value of all other
     * possible choices in combination, and so on.
     */
    RISK_AVERSE
}
