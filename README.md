# OESW - The Weakest Link challenge - app source code
The sound track : https://www.youtube.com/watch?v=VJnm43i6Rr4 

Let’s discover the distributed tracing system named Jaeger (https://github.com/jaegertracing/jaeger)
through a game inspired from the “Weakest Link” (TV game show).
The idea is during the OESW days ask anyone with a laptop, to come and join us for this game,
and we will try to interconnect our computers to build the longest chain, each of us adding
a new message.
Once the chain is up, we’ll send the initial HTTP request (JSON POST), and thanks to the Jaeger
User interface every-body will be able to take a look to the global trace and find who was the
current weakest link ! (We’ll have to tell someone to change his code…)

---

## Introduction

This small challenge will show your various tools in action :
+ [akka-http web framework](https://doc.akka.io/docs/akka-http/current/index.html)
+ [Kamon monitoring toolkit](http://kamon.io/documentation/get-started/)
+ [Jaeger distributed tracing](https://jaeger.readthedocs.io/en/latest/)
+ [Prometheus](https://prometheus.io/)
+ [Scala language](https://www.scala-lang.org/)
+ [Sbt build tool](https://www.scala-sbt.org/)

---

## Instructions
### Step 0 - Prerequisites

* Check you internet access
  + Connect to the provided 4G router
* Install SBT
  + https://www.scala-sbt.org/
* Get the `oesw-the-weak-link-app` application source code
  + `git clone https://gitlab.forge.orange-labs.fr/rhzj7430/oesw-the-weak-link-app`  

---

## Instructions
### Step 1 - Configure

* Edit `src/main/resources/application.conf`
  + change `kamon.environment.service` to your CUID 
  + Change jaeger IP from `localhost` to the IP we gave you
* Edit `src/main/scala/dummy/Dummy.scala`
  + Change `myStoryPart` text to what ever you want (with humor)
  + Change `myNeighborIp` localhost to the IP of your nearest neighbor 

---

## Instructions
### Step 2 - Start the app

* Starts the http asynchronous server :
    + `sbt run`
* Check if it works fine
    + `curl -s http://localhost:8080`
        - should return `{"status":"OK"}`

---

## Instructions
### Step 3 - Debug phasis

* Check your connectivity with your neighbor and get its story part
    ```bash
     curl -s http://localhost:8080/ask
    ```

* If something goes wrong we'll have to add logs...
  + Or check prometheus end point :
    - http://localhost:9095/

---

## Instructions
### Step 4 - Who is the weakest Link ?

* I'll send the first http POST request
* If everything goes thing :
  + Go to Jaeger UI :
    - Using provided URL 

---

## Notes for the game administrators
### Start Jaeger

```
docker run -d \
   --name jaeger \
   -e COLLECTOR_ZIPKIN_HTTP_PORT=9411 \
   -p5775:5775/udp \
   -p6831:6831/udp \
   -p6832:6832/udp \
   -p5778:5778 \
   -p16686:16686 \
   -p14268:14268 \
   -p9411:9411 \
   jaegertracing/all-in-one:latest
```

Browse to IP:16686 to reach the Jaeger user interface.

---

## Notes
### Start Prometheus & grafana

A simple prometheus.yml file for this app is in the git repository
root directory. **TAKE CARE WITH THE IP INSIDE THIS FILE**

```bash
docker run -d -p 9090:9090 \
   --name kapromotheus \
   -v $PWD/prometheus.yml:/etc/prometheus/prometheus.yml \
   prom/prometheus

docker run -d --name=grafana -p 3000:3000 grafana/grafana
```

---

## Notes for the game administrators
### Prometheus/grafana queries example

```
rate(akka_http_server_open_connections_sum[5m])
rate(akka_http_server_active_requests_sum[5m])
rate(akka_system_active_actors_sum[5m])

```


---

## Notes for the game administrators

### Ask your neightbor story part 

```bash
curl -s http://localhost:8080/ask | jq
```
### Full chain request

```bash
curl -s \
  -H 'content-type: application/json'   \
  -d '{"maxDepth":2}' \
  http://localhost:8080/chain | jq
```

