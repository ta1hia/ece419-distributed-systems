ece419
======
General assumptions
- A symbol (MSFT, etc) can be alphanumeric
- Once a server starts, it will not crash
- In Broker1, client may exit calling "x"
- In Broker2 and 3, client and exchange may exist calling "x" or "exit"

Broker3
Lookup
- "lookuptable" is a special file that the lookup uses to retrieve existing brokers
  - No broker shall be called "lookuptable"
  - "lookup" table must not be modified when the lookup is running
- On startup, there will be zero brokers registered
- We assume that once a broker (nasdaq, tse) registers to the lookup, the hostname and port shall remain the same until the server closes
  - The server can "re-register" with a different port and hostname when it starts up again

ece419

Submitted by:
Kevin Justin Gumba 997585117
Tahia Khan
