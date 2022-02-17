# HttpQueueServer

The functionality and purpose of this Java web server can be seen in the following diagram:

![](overview.png)

If there are firewall restrictions that prevent HTTP connections coming from the outside,
the HTTPQueueServer and the `HttpPollingModule` can allow that a client can still access a target server behind the firewall via HTTP.

Wrapping the HttpQueueServer in a reverse proxy like *nginx*, the server running the `HttpQueueServer` behaves for the HTTP client in the same way as the target server.
For security reasons it must be ensured via firewall and/or reverse proxy configuration that the endpoints `/pop-request` and `/response` can only be reached by the `HttpPollingModule`.

