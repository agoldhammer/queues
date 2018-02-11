queues
=======

This is a queuing similator.
--------------------------------

There are five sinks, or destinations, and ten agents. Agents may be opened or closed by clicking on them.

For each client, the agent processing time will be drawn from a random distribution whose parameters can be adjusted. Client arrival time is similarly drawn from a random distribution.

Clients will queue in the queuing area if there is no agent available to process them. After processing, each client will be dispatched to the appropriate sink.

Maximum queue length will therefore vary randomly.

Clicking the Info button will reveal average delay times for each sink, including the processing time.

A demo can be found [here](http://www.agmardor.com/public/index.html).

This project is still under development. It is written in Clojurescript with Re-frame and will run in any modern browser.
