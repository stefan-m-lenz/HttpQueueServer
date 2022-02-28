# HttpQueueServer

The functionality and purpose of this Java web server application can be seen in the following diagram:

![](overview.png)

If there are firewall restrictions that prevent HTTP connections coming from the outside,
the HTTPQueueServer and the [`HttpPollingModule`](https://github.com/stefan-m-lenz/HttpPollingModule) can allow that a client can still access a target server behind the firewall via HTTP.

Wrapping the `HttpQueueServer` in a reverse proxy like *nginx*, the server running the `HttpQueueServer` behaves for the HTTP client in the same way as the target server.
For security reasons it must be ensured via firewall and/or reverse proxy configuration that the endpoints `/pop-request` and `/response` can only be reached by the `HttpPollingModule`.

## Installation

The `HttpQueueServer` is a Java web application based on the Java/Jakarte EE 8 standard.
It can be deployed in a [Tomcat 9 web server](https://tomcat.apache.org/) or any other servlet container that supports the Java Servlet Specification 4.0.

The following script shows how the `HttpQueueServer` can be installed on an Ubuntu 18.04 server:

```bash
# Install Tomcat 9
sudo apt-get install tomcat9

# Run Tomcat as a service that will always start when the system boots up
sudo systemctl start tomcat9
sudo systemctl enable tomcat9

# Download the HttpQueueServer web archive file and deploy it on Tomcat
wget https://github.com/stefan-m-lenz/HttpQueueServer/releases/download/v1.0/HttpQueueServer.war
sudo mv HttpQueueServer.war /var/lib/tomcat9/webapps
```

For a use in production, it is necessary to install and configure a reverse proxy.

## Install and configure *nginx* as reverse proxy
Assuming the base URL of the queue server is https://queue.example.com,
the reverse proxy configuration makes it possible that answers from requests to URLs like https://queue.example.com/abc/xyz?v=123 are basically the same as when directly querying the target server via https://target.example.com/abc/xyz?v=123.
Secondly, the reverse proxy configuration must ensure that the server endpoints `/pop-request` and `/response` are only accessible by the (server running) the polling module.
Otherwise, users could intercept other users' requests.

Concretely, the reverse proxy configuration is supposed to do something like the following:

* Redirect https://queue.example.com/* to http://localhost:8080/HttpQueueServer/relay/*.
* Restrict the access to https://queue.example.com:8443/HttpQueueServer/pop-request to the IP address of the server running the polling module and redirect requests to https://queue.example.com:8443/pop-request to http://localhost:8080/HttpQueueServer/pop-request.
* Restrict the access to https://queue.example.com:8443/response to the IP address of the server running the polling module and redirect requests to https://queue.example.com:8443/response to http://localhost:8080/HttpQueueServer/response.

```bash
TODO
```

Finally, ensure that the firewall allows access to port 443, e.g. via:
```bash
ufw allow 443/tcp
```