public class FSM{

	String currentState;
	private static final int MSS = 1;////iniiiiiiiiiiiiiiiiiiiiit yadet nare
	public int cwnd;
	private int ssthresh;
	private int dupAckCount;
	private int inc = 0;
	private int winBound;

	public FSM(){
		currentState = "SlowStart";
		cwnd = MSS;
		ssthresh = 8 * MSS;
		dupAckCount = 0;
		winBound = ssthresh;
	}
	public synchronized boolean makeTransition(String input){
		System.out.println("Current state before transition " + currentState);
		if(input == "timeout"){
			currentState = "SlowStart";
			ssthresh = cwnd / 2;
			cwnd = MSS;
			dupAckCount = 0;
			System.out.println("Current state after transition " + currentState);
			return true;
		}
		switch(currentState) {
			case "SlowStart":
		    	if(input == "newack"){
		    		cwnd += MSS;
		    		dupAckCount = 0;
		    	}
		    	else if(input == "dupack"){
		    		dupAckCount++;
		    	}

		    	//change state
		    	if(cwnd > ssthresh){
		    		currentState = "CongestionAvoidance";
		    	}

		    break;

			case "CongestionAvoidance":
				if(input == "newack"){
					if(inc != winBound){
						inc++;
					}
					if(inc == winBound){
						cwnd += MSS;//(MSS * MSS / cwnd);
						inc = 0;
						winBound = cwnd;
					}
					dupAckCount = 0;
				}
		    	else if(input == "dupack"){
		    		dupAckCount++;
		    	}


		    break;

		    case "FastRecovery":
		    	if(input == "newack"){
		    		cwnd = ssthresh;
		    		dupAckCount = 0;
		    		currentState = "CongestionAvoidance";
		    	}
		    	else if(input == "dupack"){
		    		cwnd += MSS;
		    	}
		    break;

		}
		if(dupAckCount == 3){
			currentState = "FastRecovery";
    		ssthresh = cwnd / 2;
    		cwnd = ssthresh + 3*MSS;
			System.out.println("Current state after transition " + currentState);
    		return true;
		}
		System.out.println("Current state after transition " + currentState);
		return false;

	}


}
